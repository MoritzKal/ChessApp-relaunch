# Codex Local Environment

Quick start for running the ChessApp components locally without touching existing Docker or CI configs.

## Start

```bash
docker compose up -d db minio mlflow prometheus loki grafana
mvn -q -pl api spring-boot:run -Dspring-boot.run.profiles=codex
npm --prefix frontend run dev -- --mode codex
uvicorn ml.app:app --reload --port 8001
```

## Stop

```bash
docker compose down
```

## Ports & Links

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000>
- Loki: <http://localhost:3100>
- MLflow: <http://localhost:5000>

## Smoke tests

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/v3/api-docs | jq '.openapi'
curl http://localhost:8080/swagger-ui.html >/dev/null && echo "Swagger OK"
curl http://localhost:8080/actuator/prometheus | grep chs_api_http_requests_total
```

Example run (this environment lacked Docker and Maven network access):

```text
$ docker compose -f infra/docker-compose.yml up -d db minio mlflow prometheus loki grafana
bash: command not found: docker

$ mvn -q -f api/pom.xml -pl api-app spring-boot:run -Dspring-boot.run.profiles=codex
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[FATAL] Non-resolvable parent POM for com.chessapp:api:0.0.1-SNAPSHOT: The following artifacts could not be resolved: org.springframework.boot:spring-boot-starter-parent:pom:3.5.5 (absent): Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.5.5 from/to central (https://repo.maven.apache.org/maven2): Network is unreachable and 'parent.relativePath' points at no local POM @ line 4, column 13

$ curl http://localhost:8080/actuator/health
curl: (7) Failed to connect to localhost port 8080 after 0 ms: Couldn't connect to server
```

## Troubleshooting

- Install and start Docker to run the infrastructure stack.
- Ensure Maven has internet access to download dependencies.
- Optional JSON logging: run the API with `-Dlogging.config=classpath:logback-codex.xml`.
