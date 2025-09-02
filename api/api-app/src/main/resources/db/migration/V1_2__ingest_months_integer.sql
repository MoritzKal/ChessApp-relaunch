ALTER TABLE ingest_runs
    ALTER COLUMN from_month TYPE integer USING from_month::integer,
    ALTER COLUMN to_month TYPE integer USING to_month::integer;
