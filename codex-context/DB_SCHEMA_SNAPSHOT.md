DB Schema Snapshot (chsdb)

Source of truth: Flyway migrations under `api/api-app/src/main/resources/db/migration/`.
This snapshot summarizes V1 baseline.

Types

- platform: ENUM('CHESS_COM')
- game_result: ENUM('WHITE_WIN','BLACK_WIN','DRAW','ABORTED')
- color: ENUM('WHITE','BLACK')
- time_cat: ENUM('BULLET','BLITZ','RAPID','CLASSICAL')
- training_status: ENUM('QUEUED','RUNNING','SUCCEEDED','FAILED')

Tables (columns)

- users
  - id UUID PK
  - chess_username VARCHAR(64) NOT NULL
  - created_at TIMESTAMPTZ DEFAULT now()

- games
  - id UUID PK
  - user_id UUID NOT NULL → users(id)
  - platform platform NOT NULL DEFAULT 'CHESS_COM'
  - end_time TIMESTAMPTZ
  - time_control VARCHAR(32)
  - time_category time_cat
  - result game_result
  - white_rating INT, black_rating INT
  - pgn TEXT, tags JSONB
  - idx_games_user_time(user_id, end_time DESC)

- moves
  - id UUID PK
  - game_id UUID NOT NULL → games(id)
  - ply INT NOT NULL
  - san VARCHAR(20), uci VARCHAR(10)
  - color color
  - clock_ms INT, eval_cp INT, is_blunder BOOLEAN, comment TEXT
  - idx_moves_game_ply(game_id, ply)

- positions
  - id UUID PK
  - game_id UUID NOT NULL → games(id)
  - ply INT NOT NULL
  - fen TEXT NOT NULL
  - side_to_move color
  - legal_moves JSONB
  - idx_positions_game_ply(game_id, ply)

- datasets
  - id UUID PK
  - name VARCHAR(100) NOT NULL
  - version VARCHAR(50) NOT NULL
  - filter JSONB, split JSONB
  - size_rows BIGINT
  - location_uri TEXT
  - created_at TIMESTAMPTZ DEFAULT now()

- models
  - id UUID PK
  - name VARCHAR(100) NOT NULL
  - version VARCHAR(50) NOT NULL
  - framework VARCHAR(50)
  - metrics JSONB
  - artifact_uri TEXT
  - created_at TIMESTAMPTZ DEFAULT now()

- training_runs
  - id UUID PK
  - model_id UUID → models(id) ON DELETE SET NULL
  - dataset_id UUID → datasets(id) ON DELETE SET NULL
  - params JSONB
  - status training_status
  - started_at TIMESTAMPTZ, finished_at TIMESTAMPTZ
  - metrics JSONB, logs_uri TEXT

- evaluations
  - id UUID PK
  - model_id UUID → models(id) ON DELETE SET NULL
  - baseline_model_id UUID → models(id) ON DELETE SET NULL
  - metric_suite JSONB, report_uri TEXT
  - created_at TIMESTAMPTZ DEFAULT now()

How to refresh from a running DB

- Ensure DB is up: `docker compose -f infra/docker-compose.yml --env-file infra/.env up -d db`
- Inspect schema (inside container):
  - `docker exec -i chs_db psql -U chs -d chsdb -c "\dt+"`
  - `docker exec -i chs_db psql -U chs -d chsdb -c "\d+ <table>"`
- Flyway manages migrations; do not mutate schema manually.

