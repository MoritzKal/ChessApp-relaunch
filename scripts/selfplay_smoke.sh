#!/usr/bin/env bash
set -euo pipefail
curl -s localhost:8011/healthz >/dev/null
curl -s localhost:8011/runner/selfplay/debug?games=4 >/dev/null
