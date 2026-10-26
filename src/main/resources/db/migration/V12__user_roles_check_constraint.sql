-- Resolves the recurring "user_roles CHECK constraint" issue flagged repeatedly in prior
-- audits: the live Railway DB had a CHECK constraint on user_roles.role that was applied
-- manually, out of band, and never matched schema.sql or the Role.java enum — every time a
-- new Role value was added in code (most recently ROLE_ENGINEER), the app crashed on startup
-- inserting a role the DB constraint didn't know about.
--
-- This migration is now the single source of truth for that constraint. From this point on,
-- adding a new Role enum value MUST be paired with a new Flyway migration that redefines this
-- constraint — that pairing is now enforced by process (this comment), not by hoping someone
-- remembers to run a manual ALTER TABLE on Railway again.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'user_roles_role_check'
    ) THEN
        ALTER TABLE user_roles DROP CONSTRAINT user_roles_role_check;
    END IF;

    ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_check
        CHECK (role IN ('ROLE_CUSTOMER', 'ROLE_ADMIN', 'ROLE_VENDOR', 'ROLE_SUPER_ADMIN', 'ROLE_ENGINEER'));
END $$;
