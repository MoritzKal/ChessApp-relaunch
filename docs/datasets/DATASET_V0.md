# DATASET_V0 — Schema & Manifest

## Fields (row-level)
| name           | type     | required | description |
|----------------|----------|----------|-------------|
| game_id        | string   | yes      | Stable id for a game |
| ply            | int      | yes      | Half-move index (1..n) |
| fen            | string   | yes      | FEN _before_ the move |
| uci            | string   | yes      | Next move in UCI (e.g., e2e4) |
| color          | enum     | yes      | `white` or `black` (side to move) |
| result         | enum     | yes      | `white`, `black`, `draw` |
| time_control   | string   | no       | e.g., `600+0` |
| time_category  | enum     | no       | `bullet`/`blitz`/`rapid`/`classical` |
| white_rating   | int      | no       | rating at game start |
| black_rating   | int      | no       | rating at game start |
| eco            | string   | no       | ECO code if available |
| tags           | object   | no       | misc tags |

## Manifest (dataset-level)
`manifest.json`
```json
{
  "dataset_id": "ds_v0_local",
  "version": "v0",
  "created_at": "2025-08-29T16:15:00Z",
  "rows": 0,
  "location": "data/compact.parquet",
  "stats": {
    "result": {"white": 0, "black": 0, "draw": 0},
    "time_category": {"bullet": 0, "blitz": 0, "rapid": 0, "classical": 0},
    "elo_buckets": {"<800": 0, "800-1199": 0, "1200-1599": 0, "1600-1999": 0, "2000+": 0}
  }
}
```
