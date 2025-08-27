-- games: neue Spalten
ALTER TABLE games
  ADD COLUMN IF NOT EXISTS game_id_ext TEXT,
  ADD COLUMN IF NOT EXISTS white_rating INT,
  ADD COLUMN IF NOT EXISTS black_rating INT;

-- eindeutige ID für idempotente Ingests
CREATE UNIQUE INDEX IF NOT EXISTS ux_games_game_id_ext ON games(game_id_ext);

-- schneller Zugriff: (user_id, end_time DESC)
CREATE INDEX IF NOT EXISTS idx_games_user_end_time ON games(user_id, end_time DESC);

-- positions: FEN-Spalte sicherstellen
ALTER TABLE positions
  ADD COLUMN IF NOT EXISTS fen TEXT NOT NULL DEFAULT '';

-- ingest_runs Tabelle
CREATE TABLE IF NOT EXISTS ingest_runs (
  id UUID PRIMARY KEY,
  username TEXT NOT NULL,
  from_month TEXT NOT NULL,
  to_month TEXT NOT NULL,
  status TEXT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL,
  finished_at TIMESTAMPTZ,
  games_count INT DEFAULT 0,
  moves_count BIGINT DEFAULT 0,
  positions_count BIGINT DEFAULT 0,
  error TEXT,
  report_uri TEXT
);
