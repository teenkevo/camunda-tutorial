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

echo "==> Stopping services..."
for svc in "${SERVICES[@]}"; do
  pid_file="${LOG_DIR}/${svc}.pid"
  if [[ ! -f "$pid_file" ]]; then
    echo "    ${svc}: no pid file"
    continue
  fi
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    # Kill Maven parent and its Spring Boot child process group
    kill "$pid" 2>/dev/null || true
    pkill -P "$pid" 2>/dev/null || true
    echo "    ${svc}: stopped (pid ${pid})"
  else
    echo "    ${svc}: not running"
  fi
  rm -f "$pid_file"
done

if [[ "${1:-}" == "--with-db" ]]; then
  echo "==> Stopping Oracle..."
  (cd "$ROOT" && docker compose stop oracle)
fi

echo "Done."
