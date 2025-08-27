SHELL := /bin/bash

.DEFAULT_GOAL := help

ENV ?= .env

help: ## List targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' Makefile | awk 'BEGIN {FS := ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start stack
	@[ -f $(ENV) ] || (echo "Copy .env.example to .env and adjust secrets" && exit 1)
	@docker compose -f infra/docker-compose.yml --env-file $(ENV) up -d

stop: ## Stop stack
	@docker compose -f infra/docker-compose.yml --env-file $(ENV) stop

down: ## Stop & remove
	@docker compose -f infra/docker-compose.yml --env-file $(ENV) down -v

logs: ## Tail logs
	@docker compose -f infra/docker-compose.yml --env-file $(ENV) logs -f --tail=200

ps: ## Show processes
        @docker compose -f infra/docker-compose.yml --env-file $(ENV) ps

status: ## Print endpoints
        @echo "Grafana:   http://localhost:3000 (admin:$GRAFANA_USER)"
        @echo "Prometheus:http://localhost:9090"
        @echo "Loki:      http://localhost:3100"
        @echo "MinIO:     http://localhost:9001 (console)"
        @echo "MLflow:    http://localhost:5000"
        @echo "Postgres:  localhost:5432 (db=$POSTGRES_DB user=$POSTGRES_USER)"

test: ## Run monitoring connectivity tests
        @pytest infra/tests
