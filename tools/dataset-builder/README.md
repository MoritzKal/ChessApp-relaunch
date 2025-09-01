# Dataset Builder

## Quickstart

```bash
make install
make test
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
make pgn    # convert PGN files to Parquet (placeholder)
make build  # build dataset (placeholder)
make all    # run full pipeline (placeholder)
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
- Placeholder targets exist for `pgn`, `build`, and `all`.
- Future observability will expose optional `chs_dataset_rows{split}` metric via Pushgateway.

