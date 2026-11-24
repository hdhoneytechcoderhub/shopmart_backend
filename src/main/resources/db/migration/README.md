# Flyway Migrations

Flyway owns the schema. Hibernate runs with `ddl-auto: validate`, so a mapping/schema
mismatch fails startup instead of silently altering tables.

## How the two starting points work

- **Existing database (including Railway).** The live database already had a schema
  Hibernate created before Flyway was introduced. `baseline-on-migrate: true` /
  `baseline-version: 1` stamps such a non-empty database as version 1 without executing
  anything, then applies later migrations. On these databases `V1__baseline_schema.sql` is
  below the baseline and is ignored.
- **Fresh empty database.** There is no existing schema, so Flyway does not baseline and
  instead runs `V1__baseline_schema.sql`, which creates the full current schema (all 63
  tables, sequences, constraints and indexes). It then applies V2 onward normally. No
  bootstrap profile and no `ddl-auto: update` step are needed.

`V1` is a frozen snapshot of the current JPA model. Never edit it after release; put every
later change in a new, higher-versioned migration.

## Version 6 payment-table repair

V7 changes `payments` and `payment_transactions`, but neither table was ever created by a
migration in this repository — they only existed where Hibernate had auto-created them. A
database that was baselined from a partial schema reached version 6 without those tables,
so V7 failed with:

```
SQL State: 42P01
ERROR: relation "payments" does not exist
```

`V6.5__payment_tables_backfill.sql` runs between V6 and V7 and creates both tables
idempotently. V7 is already recorded in `flyway_schema_history` on affected databases, so it
must not be edited — doing so breaks checksum validation. The bridge is additive only; V7
still adds `refunded_amount` / `idempotency_key`, V19 adds the unique transaction index, and
V20 adds the payment status constraint.

## Rules

1. Never edit an applied migration (checksum validation will fail). Add a new version.
2. Migrations must be additive and safe against existing data. Use `IF NOT EXISTS` for
   tables, indexes and columns.
3. Do not reset, drop or baseline the production database over an error. Diagnose the
   missing object and add a forward migration.
4. Keep Flyway enabled and `ddl-auto` at `validate`.
5. `out-of-order` is enabled so a repair below the latest applied version (such as V6.5) can
   still run on databases that already moved past it. Repair migrations must therefore be
   fully idempotent.

## Verifying

Run both paths against disposable databases before deploying:

- **Fresh:** an empty database must apply V1 through the latest version and start with
  Hibernate validation passing.
- **Version 6:** a database with a version-1 baseline and V2–V6 applied (but no payment
  tables) must apply V6.5, V7 and the rest, then start with validation passing.
