Param()
$ErrorActionPreference = 'Stop'

function Write-Info($msg) { Write-Host $msg -ForegroundColor Cyan }

Set-Location (Resolve-Path "$PSScriptRoot/..")
New-Item -ItemType Directory -Force -Path "codex-context" | Out-Null

$SMOKE = "codex-context/SMOKE_REPORT.md"
$HEALTH = "http://localhost:8080/actuator/health"
$OPENAPI = "http://localhost:8080/v3/api-docs"

if (-not (Get-Command curl -ErrorAction SilentlyContinue)) { Write-Error "curl not found"; exit 1 }

try {
  $health = curl -fsS $HEALTH
  if ($LASTEXITCODE -ne 0) { throw "API not reachable" }
  $openapi = curl -fsS $OPENAPI
  Set-Content -Path $SMOKE -Value "# Smoke Report`nGenerated: $(Get-Date -AsUTC -Format s)Z`n`n## /actuator/health`n$health`n`n## /v3/api-docs`n$openapi" -Encoding UTF8
  $openapi | Set-Content -Path "codex-context/openapi.json" -Encoding UTF8
  Write-Info "Wrote $SMOKE and updated codex-context/openapi.json"
} catch {
  Write-Error "API not reachable on :8080"
  exit 2
}

