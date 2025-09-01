#!/usr/bin/env bash
# Manuelles, schrittweises Testen ohne make/mvnw – mit Modul-Autodetektion.
# Läuft auf Bash (auch Git Bash). Keine PR-Gates, kein Zwangs-Rebuild pro Versuch.

set -u  # kein -e, damit wir bei Testfehlern weiterlaufen

# Optional: unterdrückt deine JDK-Agent-Warnungen
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -XX:+EnableDynamicAgentLoading"

# -------------------- Modul-Autodetektion --------------------
MODULE_DIR="${MODULE_DIR:-auto}"

detect_module_dir() {
  if [[ "$MODULE_DIR" != "auto" ]]; then
    echo "$MODULE_DIR"
    return 0
  fi
  if [[ -f "api/api-app/pom.xml" ]]; then
    echo "api/api-app"
  elif [[ -f "api/pom.xml" ]]; then
    echo "api"
  elif [[ -f "pom.xml" ]]; then
    echo "."
  else
    echo ""
  fi
}

MDIR="$(detect_module_dir)"
if [[ -z "$MDIR" ]]; then
  echo "❌ Konnte kein pom.xml finden (weder in ./api/api-app, ./api noch im Root)."
  echo "   Bitte MODULE_DIR setzen: z. B. MODULE_DIR=api/api-app scripts/manual_datasets_diag.sh build"
  exit 2
fi

# -------------------- Maven-Runner mit Fallbacks --------------------
run_mvn() {
  # Alle Maven-Aufrufe im Modulverzeichnis ausführen
  if [[ -x "./mvnw" ]]; then
    ( cd "$MDIR" && ./mvnw "$@" )
  elif command -v mvn >/dev/null 2>&1; then
    ( cd "$MDIR" && mvn "$@" )
  else
    # Docker-Fallback – Workspace mounten und -w korrekt setzen (Spaces in Pfaden werden durch die Quotes gehandhabt)
    ( cd "$MDIR" && docker run --rm -v "$PWD/..":/ws -w "/ws/$(basename "$(pwd)")" maven:3-eclipse-temurin-21 mvn "$@" )
  fi
}

# -------------------- Build (einmalig, ohne Tests) --------------------
build_once() {
  echo "== Build im Modul '$MDIR' (ohne Tests) =="
  run_mvn -q -Dmaven.test.skip=true -Dspotless.skip=true -Dgpg.skip package
  rc=$?
  if [[ $rc -ne 0 ]]; then
    echo "❌ Build fehlgeschlagen (Modul: $MDIR)."
    exit $rc
  fi
  echo "✅ Build OK (Modul: $MDIR)."
}

# -------------------- Report-Helfer --------------------
last_report() {
  local rpt
  rpt=$(ls -t "$MDIR"/target/surefire-reports/*.txt 2>/dev/null | head -n 1 || true)
  if [[ -n "${rpt:-}" ]]; then
    echo -e "\n===== Kurzdiagnose: $rpt ====="
    if ! grep -E "Caused by|Exception|PSQL|org\.hibernate|org\.springframework" "$rpt" ; then
      echo "---- Voller Report ----"
      cat "$rpt"
    fi
    echo "==============================="
  else
    echo "Keine Surefire-Reports in $MDIR/target/surefire-reports gefunden."
  fi
}

# -------------------- Gemeinsame Test-Flags --------------------
SUREFIRE_FLAGS=(-q
  -Dspring.profiles.active=test
  -DfailIfNoTests=false
  -DreuseForks=true
  -DforkCount=1
  -DtrimStackTrace=false
  -Dsurefire.useFile=false
)

# -------------------- Einzelne Testläufe --------------------
test_create() {
  echo "== Test: create_shouldReturn201_withLocation_andBodyShape (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT#create_shouldReturn201_withLocation_andBodyShape test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_get() {
  echo "== Test: getById_shouldReturn200_andConsistentBody (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT#getById_shouldReturn200_andConsistentBody test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_list() {
  echo "== Test: list_shouldBePaged_andSortedByCreatedAtDesc (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT#list_shouldBePaged_andSortedByCreatedAtDesc test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_openapi() {
  echo "== Test: openapi_shouldExposeThreeDatasetEndpoints (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT#openapi_shouldExposeThreeDatasetEndpoints test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_unauth() {
  echo "== Test: unauthenticated_shouldBeRejected (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT#unauthenticated_shouldBeRejected test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_all() {
  echo "== Test: komplette DatasetsApiDiagnosticsIT (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest=DatasetsApiDiagnosticsIT test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

test_method() {
  local method="${1:-}"
  if [[ -z "$method" ]]; then
    echo "Bitte Methode angeben, z.B.: scripts/manual_datasets_diag.sh method create_shouldReturn201_withLocation_andBodyShape"
    return 2
  fi
  echo "== Test: DatasetsApiDiagnosticsIT#$method (Modul $MDIR) =="
  run_mvn "${SUREFIRE_FLAGS[@]}" -Dtest="DatasetsApiDiagnosticsIT#${method}" test
  local rc=$?; if [[ $rc -ne 0 ]]; then last_report; fi; return $rc
}

usage() {
  cat <<EOF
Usage:
  scripts/manual_datasets_diag.sh build         # einmal bauen (ohne Tests) im Modul $MDIR
  scripts/manual_datasets_diag.sh all           # komplette Diagnosesuite
  scripts/manual_datasets_diag.sh create        # nur Create-Test
  scripts/manual_datasets_diag.sh get           # nur Get-by-id
  scripts/manual_datasets_diag.sh list          # nur List (Sortierung/Paging)
  scripts/manual_datasets_diag.sh openapi       # nur OpenAPI-Prüfung
  scripts/manual_datasets_diag.sh unauth        # nur Negativtest Auth
  scripts/manual_datasets_diag.sh method <name> # beliebige Methode der IT

Tipps:
- Autodetektion Modul: bevorzugt api/api-app -> api -> Root. Überschreibe mit:
  MODULE_DIR=api/api-app scripts/manual_datasets_diag.sh create
- Bei FAIL wird automatisch der letzte Surefire-Report zusammengefasst.
EOF
}

cmd="${1:-}"
case "$cmd" in
  build) build_once;;
  all)   test_all;;
  create) test_create;;
  get)   test_get;;
  list)  test_list;;
  openapi) test_openapi;;
  unauth) test_unauth;;
  method) shift || true; test_method "${1:-}";;
  ""|help|-h|--help) usage;;
  *) echo "Unbekannter Befehl: $cmd"; usage; exit 2;;
esac
