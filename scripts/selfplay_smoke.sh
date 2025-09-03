#!/usr/bin/env bash
set -euo pipefail
curl -s -X POST localhost:8011/runner/selfplay/start -H 'content-type: application/json' \
  -d '{"modelId":"candidate_foo","baselineId":"prod","games":10,"concurrency":2,"seed":42}' | tee /tmp/run.json
RUN=$(jq -r .runId /tmp/run.json)
for i in {1..60}; do
  curl -s localhost:8011/runner/selfplay/runs/$RUN | jq .status
  sleep 2
done
