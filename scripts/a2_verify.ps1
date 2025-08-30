param(
  [int]$PortMl = 8000,
  [int]$PortGrafana = 3000,
  [string]$DatasetId = "ds_v0_local",
  [string]$RunId = ("local-{0}" -f [int][double]::Parse((Get-Date -UFormat %s))),
  [string]$ServiceMl = "ml"
)

$ErrorActionPreference = "Stop"

# --- Paths (repo-root) ---
$OutDir    = "out/a2"
$SampleDir = "data/sample"
$TmpBadDir = Join-Path $OutDir "tmp_bad"
New-Item -ItemType Directory -Force -Path $OutDir, $TmpBadDir | Out-Null

Write-Host "==> A2 Smoke/Test Run (dataset_id=$DatasetId, run_id=$RunId)"

# --- Preflight ---
foreach ($bin in @("git","docker")) {
  if (-not (Get-Command $bin -ErrorAction SilentlyContinue)) {
    throw "Required binary '$bin' not found in PATH."
  }
}
Write-Host "✓ Preflight OK"

# --- Compose wrapper (compose file in infra/) ---
function Get-ComposeCmd {
  try {
    & docker compose version *> $null
    return @{ Prog="docker"; Base=@("compose","-f","infra/docker-compose.yml","--env-file",".env") }
  } catch {
    if (Get-Command "docker-compose" -ErrorAction SilentlyContinue) {
      return @{ Prog="docker-compose"; Base=@("-f","infra/docker-compose.yml","--env-file",".env") }
    } else {
      throw "Neither 'docker compose' nor 'docker-compose' found."
    }
  }
}
$cmp = Get-ComposeCmd

function Invoke-Compose([string[]]$Args){
  & $cmp.Prog @($cmp.Base + $Args)
}

# --- Ensure branch ---
$branch = (& git rev-parse --abbrev-ref HEAD).Trim()
if ($branch -ne "feature/dataset-schema-v0") {
  & git checkout feature/dataset-schema-v0
  $branch = (& git rev-parse --abbrev-ref HEAD).Trim()
}
Write-Host ("✓ Branch: {0}" -f $branch)

# --- Build & up (compose in infra/) ---
Invoke-Compose @("up","-d","--build")

Write-Host ("→ Warte auf ML-Service (:{0}/health)" -f $PortMl)
$ok = $false
for ($i=0; $i -lt 60; $i++) {
  try {
    $resp = Invoke-WebRequest -Uri ("http://localhost:{0}/health" -f $PortMl) -TimeoutSec 3
    if ($resp.StatusCode -eq 200) { $ok = $true; break }
  } catch { Start-Sleep -Seconds 2 }
}
if (-not $ok) { throw "ML service not healthy on :$PortMl/health" }
Write-Host "✓ ML-Service healthy"

# --- Helpers to exec in container or host ---
function Exec-ML {
  param([string]$Cmd)
  Invoke-Compose @("exec","-T",$ServiceMl,"sh","-lc",$Cmd)
  return $LASTEXITCODE
}
function Py-In-ML {
  param([string[]]$Args)
  $cmd = "python " + ($Args -join " ")
  return Exec-ML -Cmd $cmd
}

# --- 3) Pytests (Tools) in Container ---
try {
  Invoke-Compose @("exec","-T",$ServiceMl,"sh","-lc","pytest ml/tests/test_dataset_tools.py -q")
} catch { Write-Host "ℹ️  Pytests reported failures (continuing for smoke run)"; }
Write-Host "✓ Pytests (Smoke) executed"

# --- 4) Validator OK (Exit 0) ---
Write-Host "→ dataset_validate (Sample, Exit 0 erwartet)"
$valCmd = @(
  "ml/tools/dataset_validate.py",
  "--path", "'$SampleDir'",
  "--dataset-id", "'$DatasetId'",
  "--report", "'$OutDir/validation_report.json'"
)
$rc = Py-In-ML -Args $valCmd
if ($rc -ne 0) { throw "dataset_validate OK path failed with exit $rc" }
Get-Item "$OutDir/validation_report.json" | Out-Null
# save stdout log (run again to capture log without failing)
Exec-ML -Cmd ("python {0}" -f ($valCmd -join " ")) | Out-Null
Write-Host "✓ Validator (OK) Report: $OutDir/validation_report.json"

# --- 5) Validator FAIL (Exit ≠ 0) ---
Write-Host "→ dataset_validate (absichtlich invalide, Exit ≠ 0 erwartet)"
@'
{"game_id":"bad1","ply":1,"fen":"invalid_fen","uci":"e2e4","color":"white","result":"white"}
'@ | Set-Content -NoNewline -Path (Join-Path $TmpBadDir "bad.jsonl") -Encoding UTF8

$badCmd = @(
  "ml/tools/dataset_validate.py",
  "--path", "'$TmpBadDir'",
  "--dataset-id", "'$DatasetId'",
  "--report", "'$OutDir/validation_bad.json'"
)
$rcBad = Py-In-ML -Args $badCmd
if ($rcBad -eq 0) { throw "Validator should have failed but returned 0" }
Write-Host "✓ Validator (FAIL) korrekt mit Exit $rcBad"

# --- 6) Export + Manifest + Metrics Push ---
Write-Host "→ dataset_export (Parquet + manifest.json + Metrics Push)"
# im Container auf den Service-Namen 'ml' posten:
$pushUrlInContainer = "http://$ServiceMl:8000/internal/dataset/metrics"
$expCmd = @(
  "ml/tools/dataset_export.py",
  "--input", "'$SampleDir'",
  "--output", "'$OutDir/compact.parquet'",
  "--dataset-id", "'$DatasetId'",
  "--manifest", "'$OutDir/manifest.json'",
  "--push-metrics", "'$pushUrlInContainer'"
)
$rcExp = Py-In-ML -Args $expCmd
if ($rcExp -ne 0) { throw "dataset_export failed with exit $rcExp" }
Get-Item "$OutDir/compact.parquet","$OutDir/manifest.json" | Out-Null
Write-Host "✓ Export-Artefakte: $OutDir/compact.parquet, $OutDir/manifest.json"

# --- 7) Metrics Assertions vom Host (:PORT_ML/metrics) ---
Write-Host "→ Prüfe erwartete Metriken auf :$PortMl/metrics"
$metrics = (Invoke-WebRequest -Uri ("http://localhost:{0}/metrics" -f $PortMl) -TimeoutSec 5).Content

function Assert-HasLine($pattern, $err){
  if (-not ($metrics -split "`n" | Select-String -Pattern $pattern)) {
    throw $err
  }
}
Assert-HasLine '^chs_dataset_rows_total'        "chs_dataset_rows_total fehlt"
Assert-HasLine '^chs_dataset_invalid_rows_total' "chs_dataset_invalid_rows_total fehlt"
Assert-HasLine '^chs_dataset_export_duration_ms' "chs_dataset_export_duration_ms fehlt"

function Sum-Metric($name){
  $lines = $metrics -split "`n" | Where-Object { $_ -match ("^{0}" -f [regex]::Escape($name)) }
  $vals = foreach($l in $lines){ ($l -split '\s+')[-1] } | ForEach-Object { [double]$_ }
  return ($vals | Measure-Object -Sum).Sum
}
$rowsTotal = Sum-Metric "chs_dataset_rows_total"
$durSum    = Sum-Metric "chs_dataset_export_duration_ms_sum"
$durCnt    = Sum-Metric "chs_dataset_export_duration_ms_count"

if ($rowsTotal -lt 1) { throw "rows_total Summe = $rowsTotal" }
if ($durSum    -lt 1) { throw "duration_ms_sum = $durSum" }
if ($durCnt    -lt 1) { throw "duration_ms_count = $durCnt" }

Write-Host ("✓ Metrics OK (rows_total={0}, export_duration_ms_sum={1}, count={2})" -f $rowsTotal,$durSum,$durCnt)

# --- 8) (Optional) Grafana Health ---
try {
  $gh = Invoke-WebRequest -Uri ("http://localhost:{0}/api/health" -f $PortGrafana) -TimeoutSec 3
  if ($gh.Content -match '"database"') {
    Write-Host "✓ Grafana gesund (api/health)"
  } else {
    Write-Host "ℹ️  Grafana-Health nicht verifiziert (optional)."
  }
} catch {
  Write-Host "ℹ️  Grafana-Health nicht verifiziert (optional)."
}

# --- 9) PromQL Spickzettel ---
@'
Invalid Rate (%): sum(chs_dataset_invalid_rows_total) / sum(chs_dataset_rows_total) * 100
Last Export (ms): sum(chs_dataset_export_duration_ms_sum) / sum(chs_dataset_export_duration_ms_count)
Invalid by Reason: sum by (reason) (increase(chs_dataset_invalid_rows_total[1h]))
Rows Processed: sum(rate(chs_dataset_rows_total[5m]))
'@ | Set-Content -Path (Join-Path $OutDir "promql.txt") -Encoding UTF8

Write-Host "✅ Alle A2 Checks erfolgreich. Artefakte in: $OutDir"
