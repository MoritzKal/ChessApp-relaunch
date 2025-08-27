CREATE TYPE platform AS ENUM ('CHESS_COM');
CREATE TYPE game_result AS ENUM ('WHITE_WIN','BLACK_WIN','DRAW','ABORTED');
CREATE TYPE color AS ENUM ('WHITE','BLACK');
CREATE TYPE time_cat AS ENUM ('BULLET','BLITZ','RAPID','CLASSICAL');
CREATE TYPE training_status AS ENUM ('QUEUED','RUNNING','SUCCEEDED','FAILED');

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  chess_username VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS games (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  platform platform NOT NULL DEFAULT 'CHESS_COM',
  end_time TIMESTAMPTZ,
  time_control VARCHAR(32),
  time_category time_cat,
  result game_result,
  white_rating INT,
  black_rating INT,
  pgn TEXT,
  tags JSONB
);
CREATE INDEX IF NOT EXISTS idx_games_user_time ON games(user_id, end_time DESC);

CREATE TABLE IF NOT EXISTS moves (
  id UUID PRIMARY KEY,
  game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  ply INT NOT NULL,
  san VARCHAR(20),
  uci VARCHAR(10),
  color color,
  clock_ms INT,
  eval_cp INT,
  is_blunder BOOLEAN,
  comment TEXT
);
CREATE INDEX IF NOT EXISTS idx_moves_game_ply ON moves(game_id, ply);

CREATE TABLE IF NOT EXISTS positions (
  id UUID PRIMARY KEY,
  game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  ply INT NOT NULL,
  fen TEXT NOT NULL,
  side_to_move color,
  legal_moves JSONB
);
CREATE INDEX IF NOT EXISTS idx_positions_game_ply ON positions(game_id, ply);

CREATE TABLE IF NOT EXISTS datasets (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  version VARCHAR(50) NOT NULL,
  filter JSONB,
  split JSONB,
  size_rows BIGINT,
  location_uri TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS models (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  version VARCHAR(50) NOT NULL,
  framework VARCHAR(50),
  metrics JSONB,
  artifact_uri TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS training_runs (
  id UUID PRIMARY KEY,
  model_id UUID REFERENCES models(id) ON DELETE SET NULL,
  dataset_id UUID REFERENCES datasets(id) ON DELETE SET NULL,
  params JSONB,
  status training_status,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  metrics JSONB,
  logs_uri TEXT
);

CREATE TABLE IF NOT EXISTS evaluations (
  id UUID PRIMARY KEY,
  model_id UUID REFERENCES models(id) ON DELETE SET NULL,
  baseline_model_id UUID REFERENCES models(id) ON DELETE SET NULL,
  metric_suite JSONB,
  report_uri TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
