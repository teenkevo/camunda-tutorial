#!/usr/bin/env bash
# Start Oracle (Docker) and all Spring Boot microservices.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

LOG_DIR="${ROOT}/.logs"
PID_DIR="${LOG_DIR}"
mkdir -p "$LOG_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required (for Oracle)." >&2
  exit 1
fi

MVN=""
if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
elif [[ -x "/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" ]]; then
  MVN="/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
else
  echo "mvn not found on PATH." >&2
  exit 1
fi

SERVICES=(
  "camunda-service"
  "governance-service"
  "refund-service"
  "taxpayer-service"
)

echo "==> Starting Oracle..."
docker compose up -d oracle

echo "==> Waiting for Oracle to become healthy..."
ATTEMPTS=60
for ((i = 1; i <= ATTEMPTS; i++)); do
  status="$(docker inspect --format='{{.State.Health.Status}}' camunda-tutorial-oracle 2>/dev/null || echo starting)"
  if [[ "$status" == "healthy" ]]; then
    echo "    Oracle is healthy."
    break
  fi
  if ((i == ATTEMPTS)); then
    echo "Oracle did not become healthy in time (last status: ${status})." >&2
    echo "Check: docker compose logs oracle" >&2
    exit 1
  fi
  sleep 5
done

echo "==> Checking service schemas..."
if ! docker compose exec -T oracle bash -lc \
  'sqlplus -s refund_service/Refund_Pass_1@//localhost:1521/FREEPDB1 <<EOF
WHENEVER SQLERROR EXIT FAILURE
SELECT 1 FROM dual;
EXIT;
EOF' >/dev/null 2>&1; then
  echo "    Schemas not ready yet — applying CREATE SESSION grants..."
  docker compose exec -T oracle bash -lc 'sqlplus -s sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba <<EOF
ALTER SESSION SET CONTAINER = FREEPDB1;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER, CREATE TYPE TO camunda_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO governance_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO refund_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO taxpayer_service;
EXIT;
EOF' >/dev/null
fi

echo "==> Building modules..."
"$MVN" -q -DskipTests package

echo "==> Starting services..."
for svc in "${SERVICES[@]}"; do
  pid_file="${PID_DIR}/${svc}.pid"
  log_file="${LOG_DIR}/${svc}.log"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "    ${svc} already running (pid $(cat "$pid_file"))"
    continue
  fi

  # Free common leftover ports if a dead pid file exists
  rm -f "$pid_file"

  nohup "$MVN" -pl "services/${svc}" -am spring-boot:run \
    >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  echo "    ${svc} starting (pid $(cat "$pid_file"), log ${log_file})"
done

echo
echo "Done. Endpoints:"
echo "  Camunda     http://localhost:8080  (demo/demo)"
echo "  Governance  http://localhost:8081/api/governance/assets"
echo "  Refunds     http://localhost:8082/api/refund-applications"
echo "  Taxpayer    http://localhost:8083/api/taxpayer-registrations"
echo
echo "Logs:  ${LOG_DIR}/"
echo "Stop:  scripts/stop-all.sh"
