# Self-Play, Evaluations & Model Registry

*Stand: 2025-09-02*

Diese Seite skizziert die Flows für Self-Play, Evaluations und die Promotion von Modellen. Die detaillierten Contracts werden in der OpenAPI gepflegt.

## Links

- [Swagger UI](/swagger-ui.html)
  - [Self-Play](/swagger-ui.html#/Self-Play)
  - [Evaluations](/swagger-ui.html#/Evaluations)
  - [Models](/swagger-ui.html#/Models)
- [OpenAPI JSON](/v3/api-docs)
  - [Self-Play](/v3/api-docs#tag/Self-Play)
  - [Evaluations](/v3/api-docs#tag/Evaluations)
  - [Models](/v3/api-docs#tag/Models)

## Beispiele

```bash
curl -X POST https://api.example.com/v1/selfplay/runs \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Idempotency-Key: <key>" \
  -H "Content-Type: application/json" \
  -d '{"modelId":"<model>","baselineId":"<id>","games":10,"concurrency":1,"seed":123}'
```

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  https://api.example.com/v1/selfplay/runs/<runId>
```

```bash
curl -X POST https://api.example.com/v1/evaluations \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Idempotency-Key: <key>" \
  -H "Content-Type: application/json" \
  -d '{"modelId":"<model>","datasetId":"<dataset>","metrics":["accuracy"]}'
```

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  https://api.example.com/v1/evaluations/<evalId>
```

```bash
curl -X POST https://api.example.com/v1/models/promote \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"modelId":"<id>"}'
```

## Hinweis

`/v1` ist stabil, keine Breaking Changes. Details siehe [ENDPOINT_STABILITY_POLICY](../ENDPOINT_STABILITY_POLICY.md) und das Contract-Board.

## Statuswerte

Runner liefern die Stati `running`, `completed` oder `failed`.

## Konfiguration

Die Basis-URLs der Runner lassen sich über die Umgebungsvariablen `SELFPLAY_RUNNER_URL` und `EVAL_RUNNER_URL` überschreiben.

