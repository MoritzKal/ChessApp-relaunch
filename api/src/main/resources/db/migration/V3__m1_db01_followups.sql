-- Rename dataset JSON columns if old names exist
ALTER TABLE IF EXISTS datasets RENAME COLUMN IF EXISTS filter TO filter_json;
ALTER TABLE IF EXISTS datasets RENAME COLUMN IF EXISTS split TO split_json;

-- Ensure ingest_runs has updated_at column and index
ALTER TABLE IF EXISTS ingest_runs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();
ALTER TABLE IF EXISTS ingest_runs ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE IF EXISTS ingest_runs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS ix_ingest_runs_updated_at ON ingest_runs(updated_at DESC);
