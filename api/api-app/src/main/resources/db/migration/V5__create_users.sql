-- Create users table to satisfy JPA mapping (follow-up, do not touch V1)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  chess_username text NOT NULL,
  created_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (chess_username)
);

-- Helpful index for lookups by username
CREATE INDEX IF NOT EXISTS idx_users_username ON users(chess_username);

