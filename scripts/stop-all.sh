#!/usr/bin/env bash
# Stop microservices started by start-all.sh (Oracle container left running).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${ROOT}/.logs"

SERVICES=(
  "camunda-service"
  "governance-service"
  "refund-service"
  "taxpayer-service"
)

if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  C_RESET=$'\033[0m'
  C_BOLD=$'\033[1m'
  C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'
  C_CYAN=$'\033[36m'
else
  C_RESET='' C_BOLD='' C_GREEN='' C_YELLOW='' C_CYAN=''
fi

ok()   { printf '%s✔%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
warn() { printf '%s⚠%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
info() { printf '%s==>%s %s%s%s\n' "$C_CYAN" "$C_RESET" "$C_BOLD" "$*" "$C_RESET"; }

info "Stopping services..."
for svc in "${SERVICES[@]}"; do
  pid_file="${LOG_DIR}/${svc}.pid"
  if [[ ! -f "$pid_file" ]]; then
    warn "${svc}: no pid file"
    continue
  fi
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    # Kill Maven parent and its Spring Boot child process group
    kill "$pid" 2>/dev/null || true
    pkill -P "$pid" 2>/dev/null || true
    ok "${svc}: stopped (pid ${pid})"
  else
    warn "${svc}: not running"
  fi
  rm -f "$pid_file"
done

if [[ "${1:-}" == "--with-db" ]]; then
  info "Stopping Oracle + OpenMetadata..."
  (cd "$ROOT" && docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml stop \
    oracle openmetadata-server openmetadata-migrate openmetadata-mysql openmetadata-elasticsearch)
  ok "Oracle + OpenMetadata stopped"
fi

ok "Done."
