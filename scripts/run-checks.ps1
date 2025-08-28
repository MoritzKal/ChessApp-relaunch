Param()
$ErrorActionPreference = 'Stop'

function Write-Info($msg) { Write-Host $msg -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host $msg -ForegroundColor Yellow }

Set-Location (Resolve-Path "$PSScriptRoot/..")
New-Item -ItemType Directory -Force -Path "codex-context" | Out-Null

Write-Info "[A] Bringing up infra services"
if (Get-Command docker -ErrorAction SilentlyContinue) {
  docker compose -f infra/docker-compose.yml --env-file infra/.env up -d db minio mlflow prometheus loki promtail grafana
} else {
  Write-Warn "docker not found; skipping infra"
}

Write-Info "[B] API tests (Maven verify incl. codex tests only)"
if (Get-Command mvn -ErrorAction SilentlyContinue) {
  mvn -f api/api-app/pom.xml `
    '-Dtest=OpenApiContractTest,ApiSmokeIT' `
    -DfailIfNoTests=false `
    -DskipITs=false -DskipTests=false `
    -Dspring-boot.repackage.skip=true `
    verify
} else {
  Write-Warn "mvn not found; skipping API tests"
}

Write-Info "[C] Frontend tests (if present)"
if (Test-Path "frontend/package.json") {
  if (Get-Command npm -ErrorAction SilentlyContinue) {
    npm --prefix frontend ci
    npm --prefix frontend test --if-present
    if ($LASTEXITCODE -ne 0) { Write-Warn "frontend tests failed or missing" }
  } else {
    Write-Warn "npm not found; skipping frontend"
  }
} else {
  Write-Info "frontend/package.json not found; skipping"
}

Write-Info "[D] ML tests (pytest)"
if (Test-Path "ml/tests") {
  if (Get-Command python -ErrorAction SilentlyContinue) {
    try { python -c "import pytest" *> $null; $hasPytest=$true } catch { $hasPytest=$false }
    if ($hasPytest) {
      python -m pytest -q ml/tests
    } else {
      Write-Info "pytest not installed; skipping ML tests"
    }
  } else {
    Write-Warn "python not found; skipping ML tests"
  }
} else {
  Write-Info "ml/tests not found; skipping"
}

Write-Info "[E] Smoke (optional) if API running on :8080"
$SMOKE = "codex-context/SMOKE_REPORT.md"
$HEALTH = "http://localhost:8080/actuator/health"
$OPENAPI = "http://localhost:8080/v3/api-docs"
if (Get-Command curl -ErrorAction SilentlyContinue) {
  try {
    $health = curl -fsS $HEALTH
    if ($LASTEXITCODE -eq 0) {
      $openapi = curl -fsS $OPENAPI
      Set-Content -Path $SMOKE -Value "# Smoke Report`nGenerated: $(Get-Date -AsUTC -Format s)Z`n`n## /actuator/health`n$health`n`n## /v3/api-docs`n$openapi" -Encoding UTF8
      $openapi | Set-Content -Path "codex-context/openapi.json" -Encoding UTF8
      Write-Info "Wrote $SMOKE and updated codex-context/openapi.json"
    } else {
      Write-Info "API not reachable; skipping smoke report"
    }
  } catch {
    Write-Info "API not reachable; skipping smoke report"
  }
} else {
  Write-Warn "curl not found; skipping smoke"
}

Write-Host "All checks finished."
