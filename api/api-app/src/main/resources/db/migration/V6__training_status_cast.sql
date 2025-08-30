-- Allow implicit cast from varchar to training_status to simplify ORM bindings
DO $$
BEGIN
    -- Create cast only if not exists (PostgreSQL lacks IF NOT EXISTS for CAST directly)
    IF NOT EXISTS (
        SELECT 1 FROM pg_cast c
        JOIN pg_type s ON c.castsource = s.oid
        JOIN pg_type t ON c.casttarget = t.oid
        WHERE s.typname = 'varchar' AND t.typname = 'training_status'
    ) THEN
        CREATE CAST (varchar AS training_status) WITH INOUT AS IMPLICIT;
    END IF;
END$$;

