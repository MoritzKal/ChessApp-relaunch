-- V2 adjusted to be idempotent and align with V1 schema.
-- Datasets table is created in V1; ensure no failure if it already exists.
CREATE TABLE IF NOT EXISTS datasets (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    filter_json TEXT NULL,
    split_json TEXT NULL,
    size_rows BIGINT NULL,
    location_uri TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes and unique constraints for datasets are defined in V1.
-- Avoid creating duplicate indexes here.
