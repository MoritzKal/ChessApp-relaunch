# Chess API

## Build
```
mvn -q -DskipTests clean package
```

## Run
Use docker compose from project root:
```
make up
```
API service starts automatically.

## Endpoints
- `GET /v1/health`
- `GET /v1/games`

## Observability
- Metrics: `GET /actuator/prometheus`
- Logs: JSON to stdout (for Loki)

## Rate Limit Policies
Default requests per minute, enforced per authenticated user unless noted:

- `/v1/selfplay/**`: 60/min
- `/v1/evaluations/**`: 60/min
- `/v1/datasets` and `/v1/ingest`: 30/min per IP
