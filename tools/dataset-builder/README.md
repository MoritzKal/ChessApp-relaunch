# Dataset Builder

## Quickstart

```bash
make -C tools/dataset-builder install
make -C tools/dataset-builder test
make -C tools/dataset-builder all IN=tests/fixtures/sample.pgn NAME=sample OUT=out
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

## Make Variables

| Variable | Description | Default |
| -------- | ----------- | ------- |
| `IN`     | Input PGN file for `pgn`/`all` | required |
| `OUT`    | Output directory for generated artifacts | `out` |
| `NAME`   | Dataset name used in manifest and upload path | `dataset` |
| `GAMES`  | Games Parquet path for `build` | `$(OUT)/parquet/games.parquet` |
| `POS`    | Positions Parquet path for `build` | `$(OUT)/parquet/positions.parquet` |
| `FILTERS`| Extra flags passed to `build_dataset.py` (filters, split ratios, seed) | – |

## Example Commands

```bash
# convert PGN to Parquet
make -C tools/dataset-builder pgn IN=tests/fixtures/sample.pgn OUT=out

# build dataset from existing Parquet outputs
make -C tools/dataset-builder build NAME=sample OUT=out

# full pipeline with metric push and S3 upload
PUSHGATEWAY_URL=http://localhost:9091 \
MINIO_ENDPOINT=localhost:9000 MINIO_ACCESS_KEY=key MINIO_SECRET_KEY=secret \
make -C tools/dataset-builder all IN=tests/fixtures/sample.pgn NAME=sample OUT=out \
FILTERS="--min-elo 1500"
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
    ├── test_e2e_dataset_builder.py
    └── test_pgn_to_parquet.py
```

## Outputs & Interpretation

- `parquet/train.parquet`, `parquet/val.parquet`, `parquet/test.parquet` — split
  datasets ready for model training.
- `manifest/dataset.json` — metadata including `name`, `version`, applied
  `filters`, `splits` and absolute `source` paths. When an upload succeeds an
  `artifact_uri` points to the uploaded tree.
- `stats/rows.json` — row counts for raw tables and each split.
- `stats/eco.json` / `stats/eco.png` — ECO code frequencies and bar chart for the
  top 20 codes.
- `stats/ply.json` / `stats/ply.png` — positions per ply as JSON and line chart.

## Troubleshooting

- *ImportError: No module named 'pyarrow'* – run `make install` to ensure all
  dependencies are available.
- *S3 upload failed* – verify `MINIO_*` credentials and that the target bucket
  exists and is writable.

## Determinism

Dataset splits are reproducible by specifying a `--seed` (default `42`). To get
consistent results across runs, keep the same seed or provide one via
`FILTERS="--seed 123"`.

## Observability

The builder emits JSON logs such as `{"event":"dataset_built",...}` and pushes
`chs_dataset_rows{split}` metrics to a Prometheus Pushgateway when
`PUSHGATEWAY_URL` is configured.

## Definition of Done

- `make install` installs pinned dependencies.
- `make test` executes the test suite.
- `make pgn` converts PGN files to Parquet format.
- `make build` joins, filters, and splits positions into train/val/test Parquet
  files.
- `make all` runs PGN conversion followed by dataset build.
- Observability pushes `chs_dataset_rows{split}` metrics and uploads artifacts to
  MinIO/S3 when credentials are provided.
