
BEGIN;

ALTER TABLE training_runs
  ALTER COLUMN metrics DROP NOT NULL;

-- Optional für spätere direkte Inserts (wir lassen die Spalte jetzt aber nullable):
 ALTER TABLE training_runs ALTER COLUMN metrics SET DEFAULT '{}'::jsonb;
 UPDATE training_runs SET metrics = '{}'::jsonb WHERE metrics IS NULL;

COMMIT;