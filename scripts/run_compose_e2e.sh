#!/usr/bin/env bash
set -euo pipefail

# ---------- 0) Windows/WSL Quirks ----------
uname_out="$(uname || true)"
if [[ "$uname_out" == MINGW* || "$uname_out" == CYGWIN* ]]; then
  export MSYS_NO_PATHCONV=1
  export COMPOSE_CONVERT_WINDOWS_PATHS=1
fi

# ---------- 1) Preflight ----------
need() { command -v "$1" >/dev/null 2>&1 || { echo "ERROR: missing tool '$1'"; exit 1; }; }
for t in docker curl jq openssl; do need "$t"; done

# ---------- 2) Repo-Root & .env ----------
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
if [[ -f .env ]]; then set -o allexport; source .env; set +o allexport; fi

# ---------- 3) Config ----------
INFRA_MAIN="${INFRA_MAIN:-infra/docker-compose.yml}"
SELFPLAY_COMPOSE="${SELFPLAY_COMPOSE:-infra/compose.selfplay.yml}"

# Runner/Eval Health via Host-Ports:
SELFPLAY_RUNNER_HOST_URL="${SELFPLAY_RUNNER_HOST_URL:-http://localhost:8011}"
EVAL_RUNNER_HOST_URL="${EVAL_RUNNER_HOST_URL:-http://localhost:8012}"

# API Base (Host-Port gemappt)
API_BASE="${API_BASE:-http://localhost:8080}"

# API → Runner/Eval (aus Sicht der API im Container!)
SELFPLAY_RUNNER_URL_FOR_API="${SELFPLAY_RUNNER_URL_FOR_API:-http://host.docker.internal:8011}"
EVAL_RUNNER_URL_FOR_API="${EVAL_RUNNER_URL_FOR_API:-http://host.docker.internal:8012}"

# Predictor für Runner (Self-Play) = API /v1/predict
SERVE_PREDICT_URL_FOR_RUNNER="${SERVE_PREDICT_URL_FOR_RUNNER:-http://host.docker.internal:8080/v1/predict}"

# Secret für JWT (gleicher Wert wie in der API)
JWT_SECRET="${API_JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_SECRET:-dev-secret}}}"

# ---------- 4) Sanity ----------
[[ -f "$SELFPLAY_COMPOSE" ]] || { echo "ERROR: $SELFPLAY_COMPOSE fehlt"; exit 1; }
[[ -f "$INFRA_MAIN"      ]] || { echo "ERROR: $INFRA_MAIN fehlt"; exit 1; }

# ---------- 5) Artifacts writable ----------
# compose.selfplay.yml mountet relativ zu infra/, daher beide Varianten anlegen.
mkdir -p artifacts/selfplay artifacts/eval || true
mkdir -p infra/artifacts/selfplay infra/artifacts/eval || true
chmod -R 0777 artifacts infra/artifacts || true

# ---------- 6) .env.selfplay erzeugen ----------
cat > .env.selfplay <<EOF
SERVE_PREDICT_URL=${SERVE_PREDICT_URL_FOR_RUNNER}
SERVE_BEARER_TOKEN=
EOF

# ---------- 7) API-Override (API→Runner/Eval über Host-Bridge) ----------
OVERRIDE_API_ENV="infra/docker-compose.override.api-env.yml"
cat > "$OVERRIDE_API_ENV" <<YML
services:
  api:
    environment:
      SELFPLAY_RUNNER_URL: ${SELFPLAY_RUNNER_URL_FOR_API}
      EVAL_RUNNER_URL: ${EVAL_RUNNER_URL_FOR_API}
    ports:
      - "8080:8080"
YML

# ---------- 8) Stacks starten ----------
echo "==> Starting Self-Play stack (Runner/Eval)…"
docker compose --env-file .env.selfplay -f "$SELFPLAY_COMPOSE" up -d --build

echo "==> Starting API…"
docker compose -f "$INFRA_MAIN" -f "$OVERRIDE_API_ENV" up -d api

# ---------- 9) Healthchecks ----------
echo "==> Health checks…"
# Dev-JWT vor dem API-Health bauen, weil Actuator geschützt sein kann.
echo "==> Minting dev JWT…"
exp=$(( $(date +%s) + 3600 ))
header='{"alg":"HS256","typ":"JWT"}'
payload=$(jq -cn --arg sub "e2e" --arg username "e2e" \
  --argjson exp "$exp" \
  --arg role1 "ADMIN" --arg role2 "USER" \
  --arg auth1 "ROLE_ADMIN" --arg auth2 "ROLE_USER" \
  '{sub:$sub,username:$username,exp:$exp,
    roles:[$role1,$role2],
    authorities:[$auth1,$auth2],
    scope:"api", scp:["api"] }')
b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
sig_input="$(printf '%s' "$header" | b64).$(printf '%s' "$payload" | b64)"
signature=$(printf '%s' "$sig_input" | openssl dgst -binary -sha256 -hmac "$JWT_SECRET" | b64)
JWT="Bearer $sig_input.$signature"

for i in {1..60}; do
  set +e
  r1=$(curl -fsS "${SELFPLAY_RUNNER_HOST_URL}/healthz" 2>/dev/null || true)
  r2=$(curl -fsS "${EVAL_RUNNER_HOST_URL}/healthz" 2>/dev/null || true)
  r3=$(curl -fsS -H "Authorization: ${JWT}" "${API_BASE}/actuator/health" 2>/dev/null || true)
  set -e
  ok1=$(echo "$r1" | jq -r '.status // empty' 2>/dev/null)
  ok2=$(echo "$r2" | jq -r '.status // empty' 2>/dev/null)
  ok3=$(echo "$r3" | jq -r '.status // empty' 2>/dev/null)
  echo "runner:${ok1:-NA} eval:${ok2:-NA} api:${ok3:-NA}"
  if [[ "$ok1" == "ok" && "$ok2" == "ok" && "$ok3" == "UP" ]]; then
    echo "Health OK"; break
  fi
  sleep 2
  [[ $i -eq 60 ]] && { echo "ERROR: health checks failed"; exit 1; }
done

# ---------- 10) Runner für Predict mit Token neu starten ----------
echo "==> Restart runner with SERVE_BEARER_TOKEN…"
cat > .env.selfplay <<EOF
SERVE_PREDICT_URL=${SERVE_PREDICT_URL_FOR_RUNNER}
SERVE_BEARER_TOKEN=${JWT#Bearer }
EOF
docker compose --env-file .env.selfplay -f "$SELFPLAY_COMPOSE" up -d --build selfplay-runner

# ---------- 11) Self-Play Start über API ----------
echo "==> POST /v1/selfplay/runs"
IDEM_KEY="$(
  { [ -r /proc/sys/kernel/random/uuid ] && cat /proc/sys/kernel/random/uuid; } \
  || { command -v uuidgen >/dev/null 2>&1 && uuidgen; } \
  || { openssl rand -hex 16; }
)"
SP_BODY='{"modelId":"staging","baselineId":"prod","games":10,"concurrency":2,"seed":42}'
SP_RESP=$(curl -fsS -X POST "${API_BASE}/v1/selfplay/runs" \
  -H "Authorization: ${JWT}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEM_KEY}" \
  -d "$SP_BODY")
echo "$SP_RESP" | jq .
runId="$(echo "$SP_RESP" | jq -r .runId)"
[[ -n "${runId:-}" && "$runId" != "null" ]] || { echo "ERROR: no runId returned"; exit 1; }

echo "==> Polling GET /v1/selfplay/runs/${runId}"
deadline=$(( $(date +%s) + 300 ))
while :; do
  ST=$(curl -fsS "${API_BASE}/v1/selfplay/runs/${runId}" -H "Authorization: ${JWT}")
  echo "$ST" | jq '{status,progress,metrics,reportUri}'
  status=$(echo "$ST" | jq -r .status)
  [[ "$status" =~ ^(completed|failed|SUCCEEDED|FAILED)$ ]] && break
  [[ $(date +%s) -gt $deadline ]] && { echo "ERROR: timeout waiting for completion"; exit 1; }
  sleep 5
done

# ---------- 12) Evaluation Start + Poll ----------
echo "==> POST /v1/evaluations"
EV_BODY='{"modelId":"staging","datasetId":"val_2025_08","metrics":["val_loss","val_acc_top1","ece"]}'
EV_RESP=$(curl -fsS -X POST "${API_BASE}/v1/evaluations" \
  -H "Authorization: ${JWT}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-ev-'"$RANDOM"'" \
  -d "$EV_BODY")
echo "$EV_RESP" | jq .
evalId="$(echo "$EV_RESP" | jq -r .evalId)"
[[ -n "${evalId:-}" && "$evalId" != "null" ]] || { echo "ERROR: no evalId returned"; exit 1; }

echo "==> Polling GET /v1/evaluations/${evalId}"
deadline=$(( $(date +%s) + 300 ))
while :; do
  ST=$(curl -fsS "${API_BASE}/v1/evaluations/${evalId}" -H "Authorization: ${JWT}")
  echo "$ST" | jq '{status,metrics,reportUri}'
  st=$(echo "$ST" | jq -r .status)
  [[ "$st" =~ ^(completed|failed|SUCCEEDED|FAILED)$ ]] && break
  [[ $(date +%s) -gt $deadline ]] && { echo "ERROR: timeout waiting for eval"; exit 1; }
  sleep 5
done

# ---------- 13) Models Promote ----------
echo "==> GET /v1/models (vor Promote)"
curl -fsS "${API_BASE}/v1/models" -H "Authorization: ${JWT}" | jq '.[] | {id,name,isProd}' || true

echo "==> POST /v1/models/promote"
curl -fsS -X POST "${API_BASE}/v1/models/promote" \
  -H "Authorization: ${JWT}" -H "Content-Type: application/json" \
  -d '{"modelId":"staging"}' | jq . || true

echo "==> GET /v1/models (nach Promote – isProd==true erwartet)"
curl -fsS "${API_BASE}/v1/models" -H "Authorization: ${JWT}" | jq '.[] | select(.isProd==true)' || true

# ---------- 14) Prometheus & OpenAPI ----------
mkdir -p outputs
curl -fsS -H "Authorization: ${JWT}" "${API_BASE}/actuator/prometheus" > outputs/prom.txt || true
curl -fsS -H "Authorization: ${JWT}" "${API_BASE}/v3/api-docs" | jq -S . > outputs/openapi.json || true

echo "==> E2E OK. Artefakte: outputs/prom.txt, outputs/openapi.json"
