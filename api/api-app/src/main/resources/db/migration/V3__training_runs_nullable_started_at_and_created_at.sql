-- V25__training_runs_nullable_started_at_and_created_at.sql
-- Goal: Allow queued runs without started_at; introduce created_at for enqueue time.

BEGIN;

-- 1) Introduce created_at (nullable first), backfill, then enforce NOT NULL + default
ALTER TABLE training_runs ADD COLUMN created_at TIMESTAMPTZ;
UPDATE training_runs
   SET created_at = COALESCE(started_at, NOW())
 WHERE created_at IS NULL;

ALTER TABLE training_runs
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN created_at SET DEFAULT NOW();

-- 2) Allow started_at to be NULL while status is QUEUED (actual guard via app metrics/alerts)
ALTER TABLE training_runs
  ALTER COLUMN started_at DROP NOT NULL;

COMMIT;
