# Flyway Migrations

This project switched from `ddl-auto: update` to Flyway-managed migrations. Because the live
Railway database already has a schema Hibernate auto-created, Flyway is configured with
`baseline-on-migrate: true` / `baseline-version: 1`.

**What this means in practice:**
- There is deliberately **no V1 migration file**. V1 represents "whatever schema already
  exists" on first deploy after this change — Flyway stamps the DB as being at version 1
  without executing anything, then runs V2 onward normally.
- On a **brand new** environment (no existing tables), baseline-on-migrate has no effect
  until it detects an empty `flyway_schema_history` and a non-empty schema; for a truly empty
  database, add a V1 migration reflecting the full current schema before deploying there, or
  let `ddl-auto` create it once and then baseline — do NOT deploy this Flyway config straight
  to a fresh empty database expecting V2+ alone to create the full schema, because it won't;
  V2+ only contains the tables/columns added in this pass.
- All migrations below (V2–V8) are strictly additive (`CREATE TABLE IF NOT EXISTS`,
  `ADD COLUMN IF NOT EXISTS`) — safe to run against the current production schema.

**Before deploying:** run these against a staging copy of the real Railway DB first. I
generated these from the JPA entity definitions, not from introspecting the live database, so
column types/defaults should be verified against what Postgres actually has (e.g. if a prior
manual `ALTER TABLE` was applied out of band, like the `user_roles` CHECK constraint fix noted
elsewhere).

## Bootstrapping a fresh empty database (e.g. local dev)

This happened for real during testing, so here is the concrete fix rather than just a warning.

Symptom: the `flyway_schema_history` table does not exist yet, Flyway baselines at version 1
(assuming the base schema already exists), then V7 fails with `relation
"payment_transactions" does not exist` because on a brand-new empty database, nothing from
before this migration set actually exists yet.

Deeper issue found while diagnosing this: 14 tables (`payment_transactions`, `wallets`,
`settings`, `service_bookings`, `service_categories`, `service_items`, `audit_logs`,
`compliance_documents`, `contact_messages`, `machines`, `order_status_history`,
`referral_codes`, `referrals`, `wallet_transactions`) have JPA entities but were never
captured in `schema.sql` or any `phase*.sql` file. They only ever existed via Hibernate's old
`ddl-auto: update` running against Railway over time, so there is no reliable script anywhere
that recreates them from scratch.

Fix: one-time bootstrap, then switch to normal mode.
1. Run once with the `bootstrap` Spring profile active (`--spring.profiles.active=bootstrap`,
   or in IntelliJ: Run > Edit Configurations > Active profiles: `bootstrap`) against your
   fresh empty database. This disables Flyway and lets Hibernate's `ddl-auto: update` create
   every table directly from the entity definitions, the same mechanism that built the
   Railway schema originally, just replayed locally instead of hand-written.
2. Once the app starts successfully, stop it.
3. Remove the `bootstrap` profile.
4. Run normally. Flyway will see no `flyway_schema_history` table, baseline at version 1
   (correct now, since the base schema really does exist), and apply V2 onward cleanly since
   `payments`, `payment_transactions`, and the rest now exist.

See `src/main/resources/application-bootstrap.yml` for exactly what the profile changes.
