-- Minimal Seed Data for local & CI smoke
INSERT INTO datasets (name, version, filter, split, size_rows, location_uri)
VALUES ('sample_ds', 'v0', '{"source":"sample"}'::jsonb, '{"train":0.8,"val":0.1,"test":0.1}'::jsonb, 4, 's3://datasets/sample_ds_v0');

INSERT INTO models (name, version, framework, artifact_uri, metrics)
VALUES ('policy_tiny', 'v0', 'pytorch', 's3://models/policy_tiny/v0/best.pt', '{"val_acc_top1":0.33}'::jsonb);

INSERT INTO training_runs (model_id, dataset_id, params, status, metrics, logs_uri)
SELECT m.id, d.id, '{"epochs":1,"batchSize":8}'::jsonb, 'SUCCEEDED', '{"loss":1.9}'::jsonb, 's3://logs/truns/demo'
FROM models m CROSS JOIN datasets d
WHERE m.name='policy_tiny' AND m.version='v0' AND d.name='sample_ds' AND d.version='v0'
LIMIT 1;

-- Demo user and game with 4 half-moves (Ruy Lopez)
WITH u AS (
  INSERT INTO users(chess_username)
  VALUES ('demo_user')
  RETURNING id
), g AS (
  INSERT INTO games(user_id, platform, game_id_ext, end_time, time_control, time_category, result, white_rating, black_rating, pgn, tags)
  VALUES ((SELECT id FROM u), 'CHESS_COM','demo-0001', now(), '5+0', 'BLITZ', 'WHITE_WIN', 1500, 1500,
          '1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 1-0',
          '{"eco":"C60","opening":"Ruy Lopez"}'::jsonb)
  RETURNING id
)
INSERT INTO moves (game_id, ply, san, uci, color)
SELECT g.id, v.ply, v.san, v.uci, v.color
FROM g CROSS JOIN (VALUES
  (1, 'e4', 'e2e4', 'WHITE'),
  (2, 'e5', 'e7e5', 'BLACK'),
  (3, 'Nf3','g1f3','WHITE'),
  (4, 'Nc6','b8c6','BLACK')
) AS v(ply, san, uci, color);
