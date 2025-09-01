-- Keep compatible with environments where V2 wasn't applied yet
CREATE TABLE IF NOT EXISTS ingest_runs (
  id         uuid PRIMARY KEY,
  username   text NOT NULL,
  range      text NULL,
  status     text NOT NULL CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
  report_uri text NULL,
  error      text NULL,
  started_at timestamptz NOT NULL DEFAULT NOW(),
  finished_at timestamptz NULL
);
CREATE INDEX IF NOT EXISTS idx_ingest_runs_started_at ON ingest_runs(started_at DESC);
