# Maintenance De-Dup Summary

## Overview
- Tests total before: 13
- Tests total after: 13
- Duplicates removed: 2
- Scripts merged: 1

## Changes
- Consolidated repeated `VALID_FEN` constants into `serve/tests/conftest.py` fixture `valid_fen`.
- Introduced `scripts/lib/smoke.sh` and reused in `scripts/run-smoke.sh` and `scripts/run-checks.sh`.

## Duplicate Detection
- `reports/test_duplicates.json`: no pairs above 85% similarity.
- `reports/script_duplicates.json`: no pairs above 85% similarity.

