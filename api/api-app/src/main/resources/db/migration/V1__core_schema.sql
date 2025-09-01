-- ChessApp DB Core Schema (V1) — Postgres 16
-- UUIDs via pgcrypto; JSONB for flexible fields.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE datasets (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name         text NOT NULL,
  version      text NOT NULL,
  filter       jsonb NOT NULL DEFAULT '{}'::jsonb,
  split        jsonb NOT NULL DEFAULT '{}'::jsonb,
  size_rows    bigint,
  location_uri text,
  created_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (name, version)
);
CREATE INDEX idx_datasets_created_at ON datasets(created_at DESC);
CREATE INDEX idx_datasets_filter_gin ON datasets USING gin (filter);
CREATE INDEX idx_datasets_split_gin  ON datasets USING gin (split);

CREATE TABLE models (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name         text NOT NULL,
  version      text NOT NULL,
  framework    text,
  metrics      jsonb NOT NULL DEFAULT '{}'::jsonb,
  artifact_uri text,
  created_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (name, version)
);
CREATE INDEX idx_models_created_at ON models(created_at DESC);
CREATE INDEX idx_models_metrics_gin ON models USING gin (metrics);

CREATE TABLE training_runs (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  model_id     uuid REFERENCES models(id) ON DELETE SET NULL,
  dataset_id   uuid REFERENCES datasets(id) ON DELETE SET NULL,
  params       jsonb NOT NULL DEFAULT '{}'::jsonb,
  status       text NOT NULL,
  started_at   timestamptz NOT NULL DEFAULT now(),
  finished_at  timestamptz,
  metrics      jsonb NOT NULL DEFAULT '{}'::jsonb,
  logs_uri     text
);
CREATE INDEX idx_tr_runs_status_started ON training_runs(status, started_at DESC);
CREATE INDEX idx_tr_runs_metrics_gin    ON training_runs USING gin (metrics);

CREATE TABLE games (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid,
  platform      text NOT NULL,
  game_id_ext   text,
  end_time      timestamptz,
  time_control  text,
  time_category text,
  result        text,
  white_rating  integer,
  black_rating  integer,
  pgn_raw       text,
  tags          jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at    timestamptz NOT NULL DEFAULT now(),
  UNIQUE (platform, game_id_ext)
);
CREATE INDEX idx_games_user_endtime ON games(user_id, end_time DESC);
CREATE INDEX idx_games_tags_gin     ON games USING gin (tags);

CREATE TABLE moves (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  game_id    uuid NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  ply        integer NOT NULL,
  san        text,
  uci        text,
  color      char(1),
  clock_ms   integer,
  eval_cp    integer,
  is_blunder boolean,
  comment    text,
  UNIQUE (game_id, ply)
);
CREATE INDEX idx_moves_game_ply ON moves(game_id, ply);

CREATE TABLE positions (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  game_id       uuid NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  ply           integer NOT NULL,
  fen           text NOT NULL,
  side_to_move  char(1),
  legal_moves   jsonb NOT NULL DEFAULT '[]'::jsonb,
  UNIQUE (game_id, ply)
);
CREATE INDEX idx_positions_game_ply ON positions(game_id, ply);
CREATE INDEX idx_positions_legal_gin ON positions USING gin (legal_moves);

CREATE TABLE evaluations (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  model_id          uuid REFERENCES models(id) ON DELETE SET NULL,
  baseline_model_id uuid REFERENCES models(id) ON DELETE SET NULL,
  metric_suite      jsonb NOT NULL DEFAULT '{}'::jsonb,
  report_uri        text,
  created_at        timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_eval_created_at ON evaluations(created_at DESC);
CREATE INDEX idx_eval_metric_suite_gin ON evaluations USING gin (metric_suite);
