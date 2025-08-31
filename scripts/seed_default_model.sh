#!/usr/bin/env bash
set -Eeuo pipefail

# Seed a dummy model artifact (best.pt) for Serve to load.
#
# Env:
#   MODEL_ID           default: default
#   MODEL_VERSION      default: 0
#   SERVE_CONTAINER    default: chs_serve
#   MODEL_ROOT         default: /models    (inside container)
#   LOCAL_MODEL_ROOT   default: ./serve_models (when no container available)
#
MODEL_ID=${MODEL_ID:-default}
MODEL_VERSION=${MODEL_VERSION:-0}
SERVE_CONTAINER=${SERVE_CONTAINER:-chs_serve}
MODEL_ROOT=${MODEL_ROOT:-/models}
LOCAL_MODEL_ROOT=${LOCAL_MODEL_ROOT:-./serve_models}

echo "[seed] Preparing artifact for modelId=${MODEL_ID} version=${MODEL_VERSION}"

if command -v docker >/dev/null 2>&1 && docker ps -a --format '{{.Names}}' | grep -qx "${SERVE_CONTAINER}"; then
  echo "[seed] Using container ${SERVE_CONTAINER}"
  docker exec -i "${SERVE_CONTAINER}" bash -lc \
    "set -e; install -Dv /dev/null '${MODEL_ROOT}/${MODEL_ID}/${MODEL_VERSION}/best.pt'"
  echo "[seed] Created ${MODEL_ROOT}/${MODEL_ID}/${MODEL_VERSION}/best.pt in container"
else
  echo "[seed] Container not found; seeding local path ${LOCAL_MODEL_ROOT}"
  install -Dv /dev/null "${LOCAL_MODEL_ROOT}/${MODEL_ID}/${MODEL_VERSION}/best.pt"
  echo "[seed] Created ${LOCAL_MODEL_ROOT}/${MODEL_ID}/${MODEL_VERSION}/best.pt locally"
  echo "[seed] Note: Set SERVE_MODEL_ROOT=${LOCAL_MODEL_ROOT} when running Serve locally."
fi

echo "[seed] Done"

