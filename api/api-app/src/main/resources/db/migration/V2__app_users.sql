-- Application accounts table for UI authentication/authorization (roles: USER, ADMIN)
CREATE TABLE IF NOT EXISTS app_users (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  username      text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  roles_csv     text NOT NULL DEFAULT 'USER',
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- Index for quick lookup by username
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_username ON app_users(username);
