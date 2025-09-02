#!/usr/bin/env bash
set -euo pipefail
python ml/eval-offline/eval.py --model-id dummy --limit 5 >/dev/null
