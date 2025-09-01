-- Follow-ups to align schema with code and be idempotent on PG16

-- 1) datasets.filter -> datasets.filter_json  (nur falls Spalte existiert)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='datasets' AND column_name='filter'
  ) THEN
    EXECUTE 'ALTER TABLE public.datasets RENAME COLUMN filter TO filter_json';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='datasets' AND column_name='split'
  ) THEN
    EXECUTE 'ALTER TABLE public.datasets RENAME COLUMN split TO split_json';
  END IF;

  -- Falls filter_json/split_json versehentlich TEXT sind, auf JSONB casten
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='datasets' AND column_name='filter_json'
      AND data_type <> 'jsonb'
  ) THEN
    EXECUTE 'ALTER TABLE public.datasets ALTER COLUMN filter_json TYPE jsonb USING filter_json::jsonb';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='datasets' AND column_name='split_json'
      AND data_type <> 'jsonb'
  ) THEN
    EXECUTE 'ALTER TABLE public.datasets ALTER COLUMN split_json TYPE jsonb USING split_json::jsonb';
  END IF;
END $$;

-- 2) Optional: GIN-Indizes auf *_json sicherstellen
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='datasets' AND column_name='filter_json'
  ) AND NOT EXISTS (
    SELECT 1 FROM pg_class WHERE relname='idx_datasets_filter_json_gin'
  ) THEN
    EXECUTE 'CREATE INDEX idx_datasets_filter_json_gin ON public.datasets USING gin (filter_json)';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='datasets' AND column_name='split_json'
  ) AND NOT EXISTS (
    SELECT 1 FROM pg_class WHERE relname='idx_datasets_split_json_gin'
  ) THEN
    EXECUTE 'CREATE INDEX idx_datasets_split_json_gin ON public.datasets USING gin (split_json)';
  END IF;
END $$;

-- 3) ingest_runs-Follow-ups nur ausführen, wenn Tabelle schon existiert
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='ingest_runs') THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns WHERE table_name='ingest_runs' AND column_name='created_at'
    ) THEN
      EXECUTE 'ALTER TABLE public.ingest_runs ADD COLUMN created_at timestamptz NOT NULL DEFAULT now()';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns WHERE table_name='ingest_runs' AND column_name='updated_at'
    ) THEN
      EXECUTE 'ALTER TABLE public.ingest_runs ADD COLUMN updated_at timestamptz NULL';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname='ix_ingest_runs_updated_at') THEN
      EXECUTE 'CREATE INDEX ix_ingest_runs_updated_at ON public.ingest_runs(updated_at DESC)';
    END IF;
  END IF;
END $$;
