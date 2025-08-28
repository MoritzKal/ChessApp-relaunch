-- Welle 4: Composite-Unique auf (platform, game_id_ext) für saubere Idempotenz
ALTER TABLE games
  ADD CONSTRAINT uq_games_platform_ext UNIQUE (platform, game_id_ext);
