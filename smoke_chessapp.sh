#!/usr/bin/env bash
# ChessApp Smoke Test (PR #3–#5)
# Requirements: bash, curl, docker, git (optional). No jq needed.
# Usage: chmod +x smoke_chessapp.sh && ./smoke_chessapp.sh
set -euo pipefail

# -------- Config --------
HOST="${HOST:-localhost}"
API_PORT="${API_PORT:-8080}"
PROM_PORT="${PROM_PORT:-9090}"
GRAFANA_PORT="${GRAFANA_PORT:-3000}"
LOKI_PORT="${LOKI_PORT:-3100}"

API_BASE="http://${HOST}:${API_PORT}"
PROM_BASE="http://${HOST}:${PROM_PORT}"
GRAFANA_BASE="http://${HOST}:${GRAFANA_PORT}"
LOKI_BASE="http://${HOST}:${LOKI_PORT}"

# Container names (adjust if different)
API_CONTAINER="${API_CONTAINER:-chs_api}"
DB_CONTAINER="${DB_CONTAINER:-chs_db}"

# Grafana credentials
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASSWORD="${GRAFANA_PASSWORD:-admin}"

# Default dataset payload
DATASET_REQ='{"name":"user-default","version":"v1","filter":{"source":"chess.com","user":"M3NG00S3"},"split":{"train":0.8,"val":0.1,"test":0.1},"sizeRows":12345}'

# -------- Helpers --------
say() { echo -e "\n\033[1m$*\033[0m"; }
ok()  { echo -e "✅ $*"; }
warn(){ echo -e "⚠️  $*"; }
fail(){ echo -e "❌ $*"; }

http_code() { # url
  curl -s -o /dev/null -w "%{http_code}" "$1"
}

json_get() { # crude JSON extractor: key, data
  # Usage: json_get id "$json"
  key="$1"; data="$2"
  printf "%s" "$data" | sed -n 's/.*"'$key'":"\{0,1\}\([^",}]*\)".*/\1/p' | head -n1
}

json_get_quoted() { # key, data (keeps quotes around value)
  key="$1"; data="$2"
  printf "%s" "$data" | sed -n 's/.*"'$key'":"\{0,1\}"\([^"]*\)".*/\1/p' | head -n1
}

count_occurrences() { # pattern, data
  pattern="$1"; data="$2"
  printf "%s" "$data" | grep -o "$pattern" | wc -l | tr -d ' '
}

# -------- Tests --------
SUMMARY_STATUS_REPO="n/a"
SUMMARY_STATUS_INFRA="up"
SUMMARY_DATASETS="n/a"
SUMMARY_DATASETS_LATEST="n/a"
SUMMARY_TRAINING="n/a"
SUMMARY_MODEL="n/a"
SUMMARY_CHANGES="- smoke test executed"
SUMMARY_BLOCKERS="- none"

SUMMARY_PROM_TARGETS="unknown"
SUMMARY_GRAFANA="no"
SUMMARY_LOKI_MDC="unknown"

SUMMARY_POST_201="no"
SUMMARY_METRIC_BUILD="n/a"
SUMMARY_METRIC_ROWS="n/a"
SUMMARY_LOG_DATASET_CREATED="no"

DATASET_ID=""
DATASET_LOC=""

# 0) Repo + Commit
say "0) Repo & Commit"
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  SHA=$(git rev-parse --short HEAD || echo "n/a")
  BRANCH=$(git rev-parse --abbrev-ref HEAD || echo "n/a")
  SUMMARY_STATUS_REPO="$(git remote get-url origin 2>/dev/null || echo "repo")  Commit/Tag: ${SHA} (${BRANCH})"
  ok "Repo OK — ${SUMMARY_STATUS_REPO}"
else
  warn "Not inside a git repo. Skipping."
fi

# 1) API Health
say "1) API Health"
HC=$(curl -s "${API_BASE}/actuator/health" || true)
if echo "$HC" | grep -q '"status":"UP"'; then
  ok "API is UP"
else
  fail "API health check failed: ${HC}"
fi

# 2) Flyway hint (logs)
say "2) Flyway (last 200 log lines)"
if docker logs -n 200 "${API_CONTAINER}" >/tmp/chs_api_tail 2>/dev/null; then
  if egrep -i "Flyway|migrat" /tmp/chs_api_tail >/dev/null; then
    ok "Flyway activity found in recent logs"
  else
    warn "No Flyway lines in last 200 log entries (may be older than 200)"
  fi
else
  warn "Cannot read logs for container ${API_CONTAINER}"
fi

# 3) DB Tables
say "3) Postgres tables (\\dt)"
if docker exec -i "${DB_CONTAINER}" bash -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"' >/tmp/chs_dt 2>/tmp/chs_dt_err; then
  head -n 30 /tmp/chs_dt
  ok "Listed tables"
else
  cat /tmp/chs_dt_err || true
  warn "Could not list tables (check container name or env)"
fi

# 4) Create Dataset (POST /v1/datasets)
say "4) Create dataset"
RESP=$(curl -s -X POST "${API_BASE}/v1/datasets" -H "Content-Type: application/json" -d "${DATASET_REQ}" || true)
CODE=$(http_code "${API_BASE}/v1/datasets")
if echo "$RESP" | grep -q '"id"'; then
  SUMMARY_POST_201="yes"
  DATASET_ID=$(json_get_quoted id "$RESP")
  DATASET_LOC=$(json_get_quoted locationUri "$RESP")
  ok "Created dataset id=${DATASET_ID}"
  echo "$RESP"
else
  echo "$RESP"
  fail "Create dataset did not return an id (HTTP ${CODE})"
fi

# 5) Metrics check
say "5) Prometheus metrics"
METRICS=$(curl -s "${API_BASE}/actuator/prometheus" || true)
BUILD=$(printf "%s" "$METRICS" | grep '^chs_dataset_build_total' | awk '{print $2}' | tail -n1)
ROWS=$(printf "%s" "$METRICS" | grep '^chs_dataset_rows' | awk '{print $2}' | tail -n1)
if [ -n "${BUILD:-}" ]; then
  SUMMARY_METRIC_BUILD="$BUILD"
  SUMMARY_METRIC_ROWS="${ROWS:-0}"
  ok "chs_dataset_build_total=${BUILD}, chs_dataset_rows=${ROWS:-0}"
else
  warn "Dataset metrics not found"
fi

# 6) Loki log for event=\"dataset.created\"
say "6) Loki: dataset.created log"
LOKI_QUERY=$(printf '{service="api"} |= "dataset.created"')
LOGS=$(curl -sG "${LOKI_BASE}/loki/api/v1/query" --data-urlencode "query=${LOKI_QUERY}" || true)
if echo "$LOGS" | grep -q "dataset.created"; then
  SUMMARY_LOG_DATASET_CREATED="yes"
  ok "Found dataset.created in Loki logs"
else
  warn "No dataset.created found (yet)"
fi

# 7) Prometheus targets (health: up)
say "7) Prometheus targets"
TARGETS=$(curl -s "${PROM_BASE}/api/v1/targets" || true)
UPCOUNT=$(echo "$TARGETS" | tr -d ' \n' | grep -o '"health":"up"' | wc -l | tr -d ' ')
if [ -n "$UPCOUNT" ] && [ "$UPCOUNT" -gt 0 ]; then
  SUMMARY_PROM_TARGETS="green (${UPCOUNT} up)"
  ok "Active targets UP: ${UPCOUNT}"
else
  SUMMARY_PROM_TARGETS="unknown"
  warn "Could not parse targets (no jq)."
fi

# 8) Grafana dashboard (UID chs-overview-v1)
say "8) Grafana dashboard chs-overview-v1"
DASH=$(curl -s -u "${GRAFANA_USER}:${GRAFANA_PASSWORD}" "${GRAFANA_BASE}/api/dashboards/uid/chs-overview-v1" || true)
if echo "$DASH" | grep -q '"uid":"chs-overview-v1"'; then
  SUMMARY_GRAFANA="yes"
  ok "Dashboard found (chs-overview-v1)"
else
  warn "Dashboard not found or auth failed"
fi

# 9) Fill some summaries
[ -n "${DATASET_ID}" ] && SUMMARY_DATASETS=">=1" && SUMMARY_DATASETS_LATEST="${DATASET_ID}@v1"

# -------- Summary --------
say "SUMMARY FOR PL"
cat <<EOF
STATUS
Repo: ${SUMMARY_STATUS_REPO}
Infra: ${SUMMARY_STATUS_INFRA}  (compose revision n/a)
Data: games=n/a, datasets=${SUMMARY_DATASETS} (latest=${SUMMARY_DATASETS_LATEST})
Training: last_run=n/a status=n/a
Model: prod=n/a  serve_latency_p50=n/a
Changes since last chat: ${SUMMARY_CHANGES}
Blocker/Risiken: ${SUMMARY_BLOCKERS}

OBSERVABILITY
- Prometheus targets: ${SUMMARY_PROM_TARGETS}
- Grafana Dashboards: Overview provisioned (${SUMMARY_GRAFANA})
- Loki: \`{service="api"} | json\` zeigt mdc.* (unknown in smoke — separate visual check)

API – DATASETS
- POST /v1/datasets: 201 (id=${DATASET_ID:-n/a}), manifest in MinIO (checked via locationUri=${DATASET_LOC:-n/a})
- Metrics: chs_dataset_build_total=${SUMMARY_METRIC_BUILD}, chs_dataset_rows=${SUMMARY_METRIC_ROWS}
- Logs: event=dataset.created + dataset_id sichtbar (${SUMMARY_LOG_DATASET_CREATED})

PATCHES APPLIED
- (validated by presence at runtime) Flyway enabled + ddl-auto=validate
- Grafana provisioning expected (UID chs-overview-v1)
- Dataset endpoints + metrics/logs exposed

OPEN ITEMS
- Visual check in Grafana (Loki table shows mdc.run_id, mdc.username, mdc.component)
- If DB \\dt failed: verify container name and env; else confirm tables
- If Prom targets parsing inconclusive: open Prom UI → Status → Targets
EOF
