#!/usr/bin/env bash
set -euo pipefail
python -m ml.eval-offline.eval --model-id candidate_foo --dataset /tmp/mini.parquet --limit 100
