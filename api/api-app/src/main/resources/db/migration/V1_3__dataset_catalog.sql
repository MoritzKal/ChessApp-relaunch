-- Add columns for dataset catalog and versions table
ALTER TABLE datasets
    ADD COLUMN IF NOT EXISTS size_bytes bigint,
    ADD COLUMN IF NOT EXISTS updated_at timestamptz;

CREATE TABLE IF NOT EXISTS dataset_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id uuid NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    version text NOT NULL,
    rows bigint,
    size_bytes bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (dataset_id, version)
);

ALTER TABLE ingest_runs
    ADD COLUMN IF NOT EXISTS versions jsonb;
