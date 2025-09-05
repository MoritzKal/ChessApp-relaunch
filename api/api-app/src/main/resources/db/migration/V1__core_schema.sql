-- ChessApp DB Core Schema (V1) — Postgres 16
-- UUIDs via pgcrypto; JSONB for flexible fields.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enum types
CREATE TYPE platform AS ENUM ('CHESS_COM');
CREATE TYPE time_category AS ENUM ('BULLET', 'BLITZ', 'RAPID', 'CLASSICAL');
CREATE TYPE game_result AS ENUM ('WHITE_WIN', 'BLACK_WIN', 'DRAW', 'ABORTED');
CREATE TYPE training_status AS ENUM ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED');
CREATE TYPE color AS ENUM ('WHITE', 'BLACK');

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

CREATE TABLE users (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  chess_username  text NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_users_chess_username ON users(chess_username);

CREATE TABLE training_runs (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  model_id     uuid REFERENCES models(id) ON DELETE SET NULL,
  dataset_id   uuid REFERENCES datasets(id) ON DELETE SET NULL,
  params       jsonb NOT NULL DEFAULT '{}'::jsonb,
  status       training_status NOT NULL,
  started_at   timestamptz NOT NULL DEFAULT now(),
  finished_at  timestamptz,
  metrics      jsonb NOT NULL DEFAULT '{}'::jsonb,
  logs_uri     text
);
CREATE INDEX idx_tr_runs_status_started ON training_runs(status, started_at DESC);
CREATE INDEX idx_tr_runs_metrics_gin    ON training_runs USING gin (metrics);

CREATE TABLE games (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  platform      platform NOT NULL,
  game_id_ext   text,
  end_time      timestamptz,
  time_control  text,
  time_category time_category,
  result        game_result,
  white_rating  integer,
  black_rating  integer,
  pgn           text NOT NULL,
  tags          jsonb NOT NULL DEFAULT '{}'::jsonb,
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
  color      color NOT NULL,
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
  side_to_move  color NOT NULL,
  legal_moves   jsonb NOT NULL DEFAULT '[]'::jsonb,
  UNIQUE (game_id, ply)
);
CREATE INDEX idx_positions_game_ply ON positions(game_id, ply);
CREATE INDEX idx_positions_legal_gin ON positions USING gin (legal_moves);

CREATE TABLE ingest_runs (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  username        text NOT NULL,
  from_month      text NOT NULL,
  to_month        text NOT NULL,
  status          text NOT NULL,
  started_at      timestamptz NOT NULL,
  finished_at     timestamptz,
  dataset_id      text,
  version         text,
  files_written   bigint DEFAULT 0,
  games_count     bigint DEFAULT 0,
  moves_count     bigint DEFAULT 0,
  positions_count bigint DEFAULT 0,
  error           text,
  report_uri      text
);
CREATE INDEX idx_ingest_runs_started ON ingest_runs(started_at DESC);

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
