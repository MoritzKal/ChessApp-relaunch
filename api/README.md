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
