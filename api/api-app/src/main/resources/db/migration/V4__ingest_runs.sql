CREATE TABLE ingest_runs (
  id UUID PRIMARY KEY,
  username TEXT NOT NULL,
  range TEXT NULL,
  status TEXT NOT NULL CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
  report_uri TEXT NULL,
  error TEXT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ NULL
);
CREATE INDEX idx_ingest_runs_started_at ON ingest_runs(started_at DESC);
