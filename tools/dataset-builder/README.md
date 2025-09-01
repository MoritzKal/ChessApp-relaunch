# Dataset Builder

## Quickstart

```bash
make install
make test
make pgn IN=tests/fixtures/sample.pgn OUT=out
```

## Environment Variables

| Name | Description |
| ---- | ----------- |
| `PUSHGATEWAY_URL` | URL for optional Prometheus Pushgateway (placeholder) |
| `MINIO_ENDPOINT`  | MinIO server endpoint (placeholder) |
| `MINIO_ACCESS_KEY`| MinIO access key (placeholder) |
| `MINIO_SECRET_KEY`| MinIO secret key (placeholder) |

## Example Commands

```bash
make pgn IN=tests/fixtures/sample.pgn OUT=out  # convert PGN to Parquet
make build                                    # build dataset (placeholder)
make all                                      # run full pipeline (placeholder)
```

## Project Structure

```
tools/dataset-builder/
├── Makefile
├── README.md
├── requirements.txt
└── tests
    ├── __init__.py
    └── fixtures
        └── sample.pgn
```

## Definition of Done

- `make install` installs pinned dependencies.
- `make test` executes the test suite.
- `make pgn` converts PGN files to Parquet format.
- Placeholder targets exist for `build` and `all`.
- Future observability will expose optional `chs_dataset_rows{split}` metric via Pushgateway.

