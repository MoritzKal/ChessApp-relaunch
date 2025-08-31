-- Align schema for datasets and ingest_runs

-- adjust dataset columns
ALTER TABLE datasets RENAME COLUMN filter TO filter_json;
ALTER TABLE datasets ALTER COLUMN filter_json TYPE TEXT USING filter_json::text;
ALTER TABLE datasets RENAME COLUMN split TO split_json;
ALTER TABLE datasets ALTER COLUMN split_json TYPE TEXT USING split_json::text;
CREATE INDEX IF NOT EXISTS ix_datasets_created_at ON datasets(created_at DESC);

-- recreate ingest_runs with simplified columns
DROP TABLE IF EXISTS ingest_runs;
CREATE TABLE ingest_runs (
  run_id UUID PRIMARY KEY,
  status TEXT NOT NULL CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
  report_uri TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_ingest_runs_updated_at ON ingest_runs(updated_at DESC);
