#!/usr/bin/env bash
# Stop microservices started by start-all.sh.
# Oracle and OpenMetadata keep running unless requested:
#   ./scripts/stop-all.sh [--with-db] [--with-om]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN_DIR="${TMPDIR:-/tmp}/camunda-tutorial"
LOG_DIR="${RUN_DIR}"

SERVICES=(
  "camunda-service"
  "governance-service"
  "refund-service"
  "registration-service"
  "document-service"
  "authority-service"
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

service_port() {
  case "$1" in
    camunda-service) echo 8080 ;;
    governance-service) echo 8081 ;;
    refund-service) echo 8082 ;;
    registration-service) echo 8083 ;;
    document-service) echo 8085 ;;
    authority-service) echo 8086 ;;
    *) echo "" ;;
  esac
}

# spring-boot:run records a short-lived launcher pid. The Maven JVM and the
# forked application stay in that process group after the launcher exits.
collect_service_pids() {
  local svc="$1"
  local pid_file="$2"
  local -a found=()
  local pid cmd port listener

  if [[ -f "$pid_file" ]]; then
    pid="$(tr -d '[:space:]' <"$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]]; then
      if kill -0 "$pid" 2>/dev/null; then
        found+=("$pid")
      fi
      while IFS= read -r listener; do
        [[ -n "$listener" ]] && found+=("$listener")
      done < <(pgrep -g "$pid" 2>/dev/null || true)
    fi
  fi

  while IFS= read -r pid; do
    [[ -n "$pid" ]] || continue
    cmd="$(ps -o command= -p "$pid" 2>/dev/null || true)"
    if [[ "$cmd" == *"services/${svc}"* && ( "$cmd" == *"${ROOT}"* || "$cmd" == *"spring-boot:run"* ) ]]; then
      found+=("$pid")
    fi
  done < <(pgrep -f "services/${svc}" 2>/dev/null || true)

  port="$(service_port "$svc")"
  if [[ -n "$port" ]]; then
    while IFS= read -r listener; do
      [[ -n "$listener" ]] || continue
      cmd="$(ps -o command= -p "$listener" 2>/dev/null || true)"
      if [[ "$cmd" == *"${ROOT}"* || "$cmd" == *"services/${svc}"* ]]; then
        found+=("$listener")
      fi
    done < <(lsof -nP -iTCP:"${port}" -sTCP:LISTEN -t 2>/dev/null || true)
  fi

  if [[ ${#found[@]} -eq 0 ]]; then
    return 0
  fi
  printf '%s\n' "${found[@]}" | awk 'NF && !seen[$0]++'
}

kill_tree() {
  local pid="$1"
  local child
  while IFS= read -r child; do
    [[ -n "$child" ]] && kill_tree "$child"
  done < <(pgrep -P "$pid" 2>/dev/null || true)
  kill "$pid" 2>/dev/null || true
}

info "Stopping services..."
for svc in "${SERVICES[@]}"; do
  pid_file="${LOG_DIR}/${svc}.pid"
  pids=()
  while IFS= read -r pid; do
    [[ -n "$pid" ]] && pids+=("$pid")
  done < <(collect_service_pids "$svc" "$pid_file")

  if [[ ${#pids[@]} -eq 0 ]]; then
    if [[ -f "$pid_file" ]]; then
      warn "${svc}: not running"
    else
      warn "${svc}: no pid file"
    fi
    rm -f "$pid_file"
    continue
  fi

  for pid in "${pids[@]}"; do
    kill_tree "$pid"
  done

  # Forked JVMs can outlive a parent that was signalled first. Force leftovers.
  sleep 1
  while IFS= read -r pid; do
    [[ -n "$pid" ]] && kill -9 "$pid" 2>/dev/null || true
  done < <(collect_service_pids "$svc" "$pid_file")

  rm -f "$pid_file"
  ok "${svc}: stopped (pid ${pids[*]})"
done

STOP_DB=0
STOP_OM=0
for arg in "$@"; do
  case "$arg" in
    --with-db) STOP_DB=1 ;;
    --with-om) STOP_OM=1 ;;
    *)
      warn "Unknown option: ${arg}"
      warn "Usage: scripts/stop-all.sh [--with-db] [--with-om]"
      exit 1
      ;;
  esac
done

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml)

if [[ "$STOP_DB" -eq 1 ]]; then
  info "Stopping Oracle..."
  (cd "$ROOT" && "${COMPOSE[@]}" stop oracle)
  ok "Oracle stopped"
fi

if [[ "$STOP_OM" -eq 1 ]]; then
  info "Stopping OpenMetadata..."
  (cd "$ROOT" && "${COMPOSE[@]}" stop \
    openmetadata-server openmetadata-ingestion openmetadata-migrate \
    openmetadata-mysql openmetadata-elasticsearch)
  ok "OpenMetadata stopped"
fi

ok "Done."
