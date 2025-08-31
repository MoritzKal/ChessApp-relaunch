#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
pushd "$ROOT_DIR" >/dev/null

mkdir -p codex-context
source scripts/lib/smoke.sh

generate_smoke_report || exit $?

popd >/dev/null

