# Shared helpers for demo scripts. Source from scripts/demo/*.sh
# shellcheck shell=bash

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml)

OM_SERVICES=(
  openmetadata-server
  openmetadata-ingestion
  openmetadata-migrate
  openmetadata-mysql
  openmetadata-elasticsearch
)

OM_VOLUMES=(
  camunda-tutorial_openmetadata-mysql-data
  camunda-tutorial_openmetadata-es-data
  camunda-tutorial_openmetadata-ingestion-dag-airflow
  camunda-tutorial_openmetadata-ingestion-dags
  camunda-tutorial_openmetadata-ingestion-tmp
)

if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  C_RESET=$'\033[0m'
  C_BOLD=$'\033[1m'
  C_DIM=$'\033[2m'
  C_RED=$'\033[31m'
  C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'
  C_CYAN=$'\033[36m'
else
  C_RESET='' C_BOLD='' C_DIM='' C_RED='' C_GREEN='' C_YELLOW='' C_CYAN=''
fi

ok()   { printf '%s✔%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
fail() { printf '%s✖%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; }
warn() { printf '%s⚠%s %s\n' "$C_YELLOW" "$C_RESET" "$*" >&2; }
info() { printf '%s==>%s %s%s%s\n' "$C_CYAN" "$C_RESET" "$C_BOLD" "$*" "$C_RESET"; }
step() { printf '    %s\n' "$*"; }
dim()  { printf '    %s%s%s\n' "$C_DIM" "$*" "$C_RESET"; }

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    fail "docker is required."
    exit 1
  fi
}

wait_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-60}"
  local expect="${4:-200}"
  local i code
  for ((i = 1; i <= attempts; i++)); do
    code="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "$url" 2>/dev/null || true)"
    if [[ "$code" == "$expect" ]]; then
      ok "${name} is up (HTTP ${code})"
      return 0
    fi
    if ((i % 6 == 0)); then
      dim "waiting for ${name} (attempt ${i}/${attempts}, http=${code})..."
    fi
    sleep 5
  done
  fail "Timed out waiting for ${name} (${url})"
  return 1
}
