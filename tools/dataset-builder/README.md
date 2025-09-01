# Dataset Builder

## Quickstart

```bash
make install
make test
make pgn IN=tests/fixtures/sample.pgn OUT=out
make build GAMES=out/parquet/games.parquet POS=out/parquet/positions.parquet NAME=sample OUT=out
```

## Environment Variables

| Name | Description |
| ---- | ----------- |
| `PUSHGATEWAY_URL` | Optional Prometheus Pushgateway URL for metric push |
| `MINIO_ENDPOINT`  | Optional MinIO/S3 endpoint host:port |
| `MINIO_ACCESS_KEY`| Access key for MinIO/S3 upload |
| `MINIO_SECRET_KEY`| Secret key for MinIO/S3 upload |
| `MINIO_BUCKET`    | Target bucket (default `chess-datasets`) |
| `MINIO_SECURE`    | Use HTTPS when `1`, HTTP otherwise |

## Example Commands

```bash
make pgn IN=tests/fixtures/sample.pgn OUT=out  # convert PGN to Parquet
make build GAMES=out/parquet/games.parquet POS=out/parquet/positions.parquet NAME=sample OUT=out
# push metrics and upload artifacts
PUSHGATEWAY_URL=http://localhost:9091 \
MINIO_ENDPOINT=localhost:9000 MINIO_ACCESS_KEY=key MINIO_SECRET_KEY=secret \
make build GAMES=out/parquet/games.parquet POS=out/parquet/positions.parquet NAME=sample OUT=out
make all                                      # run full pipeline (placeholder)
```

## Project Structure

```
tools/dataset-builder/
├── Makefile
├── README.md
├── build_dataset.py
├── pgn_to_parquet.py
├── requirements.txt
└── tests
    ├── __init__.py
    ├── fixtures
    │   └── sample.pgn
    ├── test_build_dataset.py
    └── test_pgn_to_parquet.py
```

## Outputs & Interpretation

- `manifest/dataset.json` — metadata about the dataset build including `name`,
  `version`, applied `filters`, `splits` with fractions and row counts, absolute
  `source` paths and the `created_at` timestamp. When an upload succeeds the
  manifest additionally contains an `artifact_uri` pointing to the uploaded tree.
- `stats/rows.json` — row counts for raw tables and each split.
- `stats/eco.json` — ECO code frequencies; `stats/eco.png` visualises the top 20
  ECO codes as a bar chart.
- `stats/ply.json` — positions per ply; `stats/ply.png` plots these counts as a
  line chart.

## Definition of Done

- `make install` installs pinned dependencies.
- `make test` executes the test suite.
- `make pgn` converts PGN files to Parquet format.
- `make build` joins, filters, and splits positions into train/val/test Parquet files.
- Placeholder target exists for `all`.
- Optional observability pushes `chs_dataset_rows{split}` metrics to a Pushgateway
  and uploads artifacts to MinIO/S3 when credentials are provided.

