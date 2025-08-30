--- /dev/null
 b/scripts/obs_e2e_test.sh
@@
#!/usr/bin/env bash
set -euo pipefail

# Usage: bash scripts/obs_e2e_test.sh
# Erwartung: Docker-Compose Stack läuft lokal (infra/docker-compose.yml).

DC="-f infra/docker-compose.yml"
PROM="http://localhost:9090"
LOKI="http://localhost:3100"
GRAF="http://localhost:3000"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-admin}"
SVC_PROM="prometheus"
SVC_PROMTAIL="promtail"
SVC_API="api"

# Helper
q(){ curl -sG --data-urlencode "query=$1" "$PROM/api/v1/query"; }
qv(){ q "$1" | jq -r '.data.result[].value[1]' ;}
hr(){ printf '%*s\n' "${COLUMNS:-80}" '' | tr ' ' -; }
fail(){ echo "❌ $*"; exit 1; }
ok(){ echo "✅ $*"; }

echo "== A3 E2E Smoke Test =="
hr

echo "[1/6] Prometheus readiness  Targets"
curl -sf "$PROM/-/ready" >/dev/null || fail "Prometheus not ready"
[[ "$(qv 'up{job=\"api\"}')" == "1" ]]  || fail "Target api != 1"
[[ "$(qv 'up{job=\"ml\"}')" == "1" ]]   || fail "Target ml != 1"
[[ "$(qv 'up{job=\"serve\"}')" == "1" ]]|| fail "Target serve != 1"
ok "Prometheus ready & targets up"

echo "[2/6] Rule syntax & unit tests"
docker compose $DC exec $SVC_PROM /bin/promtool check rules /etc/prometheus/alerts/selfplay.yml >/dev/null
docker compose $DC exec $SVC_PROM /bin/promtool check rules /etc/prometheus/alerts/dataset.yml >/dev/null
docker compose $DC exec $SVC_PROM /bin/promtool test rules /etc/prometheus/alerts/tests/selfplay.test.yml >/dev/null
docker compose $DC exec $SVC_PROM /bin/promtool test rules /etc/prometheus/alerts/tests/dataset.test.yml >/dev/null
ok "Prometheus rules: syntax OK  tests PASS"

echo "[3/6] Loki MDC label ingest"
# Sanity: promtail läuft & hat docker.sock mounted?
docker compose $DC exec $SVC_PROMTAIL sh -lc 'test -S /var/run/docker.sock' \
  || fail "promtail: docker.sock missing"

RUNID="run-$RANDOM"; DSID="ds-e2e"
# Länger laufende Probe, damit Discovery sicher greift
docker rm -f logprobe >/dev/null 2>&1 || true
docker run -d --name logprobe --label promtail.scrape=true busybox sh -c \
  'i=0; while [ $i -lt 20 ]; do echo "{\"level\":\"INFO\",\"message\":\"mdc-probe\",\"run_id\":\"'"$RUNID"'\",\"dataset_id\":\"'"$DSID"'\",\"model_id\":\"m0\",\"model_version\":\"0\",\"username\":\"local\",\"component\":\"training\"}"; i=$((i+1)); sleep 1; done' >/dev/null

# Poll Loki bis Treffer (max ~20s)
found="0"
for i in {1..20}; do
  sleep 1
  found="$(curl -sG "$LOKI/loki/api/v1/query" \
    --data-urlencode "query={run_id=\"$RUNID\",dataset_id=\"$DSID\"}" \
    --data-urlencode "limit=5" | jq -r '.data.result | length')"
  if [[ "$found" -ge 1 ]]; then break; fi
done
docker rm -f logprobe >/dev/null 2>&1 || true
[[ "$found" -ge 1 ]] || fail "Loki MDC labels not found (run_id=$RUNID, dataset_id=$DSID)"
ok "Loki MDC labels OK ($found result(s))"

echo "[4/6] Prometheus queries respond (sanity)"
for EXPR in \
  'sum(rate(chs_selfplay_games_total[1m]))' \
  '100 * (sum(increase(chs_selfplay_wins_total[15m])) / sum(increase(chs_selfplay_games_total[15m])))' \
  'sum(rate(chs_selfplay_failures_total[5m]))' \
  'histogram_quantile(0.95, sum by (le) (rate(chs_dataset_export_duration_seconds_bucket[5m])))'
do
  status=$(q "$EXPR" | jq -r '.status')
  [[ "$status" == "success" ]] || fail "Prometheus query failed: $EXPR"
done
ok "Prometheus expressions responded"

echo "[5/6] Grafana dashboards by UID"
title1="$(curl -sf -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAF/api/dashboards/uid/selfplay-health" | jq -r '.dashboard.title')"
title2="$(curl -sf -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAF/api/dashboards/uid/dataset-quality" | jq -r '.dashboard.title')"
[[ "$title1" == "Self-Play Health" ]]   || fail "Grafana selfplay-health not found"
[[ "$title2" == "Dataset Quality" ]]    || fail "Grafana dataset-quality not found"
ok "Grafana dashboards present"

echo "[6/6] Rules loaded in Prometheus"
curl -s "$PROM/api/v1/rules" | jq -e '.data.groups[].name' | grep -q '"selfplay"' || fail "Missing selfplay rule group"
curl -s "$PROM/api/v1/rules" | jq -e '.data.groups[].name' | grep -q '"dataset"'   || fail "Missing dataset rule group"
ok "Rule groups visible"

hr
echo "🎯 A3 Observability E2E Smoke: PASS"
