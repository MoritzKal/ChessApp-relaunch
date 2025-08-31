CREATE TABLE datasets (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    filter JSONB NULL,
    split JSONB NULL,
    size_rows BIGINT NULL,
    location_uri TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_datasets_name_version ON datasets(name, version);
CREATE INDEX ix_datasets_created_at ON datasets(created_at DESC);
