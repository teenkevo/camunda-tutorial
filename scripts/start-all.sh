#!/usr/bin/env bash
# Start Oracle (Docker) and all Spring Boot microservices, then print governance
# conformance and Bean Validation constraints sniffed from @Valid request DTOs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Ephemeral run state (pids/logs) — not kept in the repo; can be rewired later.
RUN_DIR="${TMPDIR:-/tmp}/camunda-tutorial"
LOG_DIR="${RUN_DIR}"
PID_DIR="${RUN_DIR}"
mkdir -p "$RUN_DIR"

GOVERNANCE_URL="${GOVERNANCE_URL:-http://localhost:8081}"

# Colors (Docker Compose–style). Disabled when not a TTY or NO_COLOR is set.
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

if ! command -v docker >/dev/null 2>&1; then
  fail "docker is required (for Oracle)."
  exit 1
fi

MVN=""
if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
elif [[ -x "/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" ]]; then
  MVN="/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
else
  fail "mvn not found on PATH."
  exit 1
fi

# Domain services publish catalog metadata on startup — start governance first.
BOOTSTRAP_SERVICES=(
  "governance-service"
)
DOMAIN_SERVICES=(
  "camunda-service"
  "refund-service"
  "registration-service"
)
SERVICES=(
  "${BOOTSTRAP_SERVICES[@]}"
  "${DOMAIN_SERVICES[@]}"
)

# Wait until Spring has logged startup AND the URL accepts a TCP/HTTP connection.
# Any HTTP status (including 4xx/5xx) counts as "up"; connection refused does not.
wait_service() {
  local svc="$1"
  local url="$2"
  local attempts="${3:-90}"
  local log_file="${LOG_DIR}/${svc}.log"
  local pid_file="${PID_DIR}/${svc}.pid"

  for ((i = 1; i <= attempts; i++)); do
    if [[ -f "$pid_file" ]] && ! kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      fail "${svc} process exited early. Last log lines:"
      tail -n 40 "$log_file" 2>/dev/null || true
      return 1
    fi

    local started=0
    if [[ -f "$log_file" ]] && grep -Eq "Started .+Application in " "$log_file" 2>/dev/null; then
      started=1
    fi

    local code
    code="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 1 "$url" 2>/dev/null || true)"
    if [[ -z "$code" ]]; then
      code="000"
    fi

    if [[ "$started" -eq 1 && "$code" != "000" ]]; then
      ok "${svc} is up (HTTP ${code})"
      return 0
    fi

    if ((i % 10 == 0)); then
      dim "still waiting for ${svc} (attempt ${i}/${attempts}, http=${code}, started=${started})..."
    fi
    sleep 3
  done

  fail "Timed out waiting for ${svc} (${url}). Last log lines:"
  tail -n 60 "$log_file" 2>/dev/null || true
  return 1
}

print_conformance() {
  local service="$1"
  local url="${GOVERNANCE_URL}/api/governance/conformance?service=${service}"
  echo
  info "Governance conformance: ${service}"
  if ! body="$(curl -sf "$url")"; then
    fail "FAILED to call ${url}"
    return 1
  fi

  if command -v python3 >/dev/null 2>&1; then
    # Pass colors into Python so compliant true/false get green ✔ / red ✖
    GREEN="$C_GREEN" RED="$C_RED" RESET="$C_RESET" DIM="$C_DIM" \
    python3 - "$body" <<'PY'
import json, os, sys

raw = sys.argv[1]
report = json.loads(raw)
ok = bool(report.get("compliant"))
green = os.environ.get("GREEN", "")
red = os.environ.get("RED", "")
reset = os.environ.get("RESET", "")
dim = os.environ.get("DIM", "")

if ok:
    print(f"    compliant: {green}✔ true{reset}")
else:
    print(f"    compliant: {red}✖ false{reset}")

issues = report.get("issues") or []
if not issues:
    print(f"    issues: {dim}(none){reset}")
else:
    print(f"    issues ({len(issues)}):")
    for issue in issues:
        entity = issue.get("entityName") or ""
        field = issue.get("fieldName")
        loc = f"{entity}.{field}" if field else entity
        rule = issue.get("rule") or "?"
        msg = issue.get("message") or ""
        print(f"      {red}✖{reset} [{rule}] {loc}: {msg}")
sys.exit(0 if ok else 2)
PY
  else
    step "${body}"
  fi
}

# Bean Validation constraints sniffed from @Valid @RequestBody DTOs at domain startup.
print_validations() {
  local service="$1"
  local url="${GOVERNANCE_URL}/api/governance/validations?service=${service}"
  echo
  info "Request validations sniffed: ${service}"
  dim "Source: @Valid @RequestBody DTOs → id {service}.{Dto}.{field}.{Constraint}"
  if ! body="$(curl -sf "$url")"; then
    fail "FAILED to call ${url}"
    return 1
  fi

  if command -v python3 >/dev/null 2>&1; then
    GREEN="$C_GREEN" RESET="$C_RESET" DIM="$C_DIM" CYAN="$C_CYAN" \
    python3 - "$body" <<'PY'
import json, os, sys

rows = json.loads(sys.argv[1])
green = os.environ.get("GREEN", "")
reset = os.environ.get("RESET", "")
dim = os.environ.get("DIM", "")
cyan = os.environ.get("CYAN", "")

if not rows:
    sys.exit(0)

print(f"    count: {green}{len(rows)}{reset}")
for row in rows:
    vid = row.get("validationId") or "?"
    field = row.get("fieldName") or ""
    constraint = row.get("constraintName") or ""
    path = row.get("requestPath") or ""
    method = row.get("httpMethod") or ""
    msg = row.get("message") or ""
    where = f"{method} {path}".strip()
    detail = f"{field}.{constraint}" if field and constraint else constraint or field
    print(f"      {cyan}•{reset} {vid}")
    extra = []
    if where:
        extra.append(where)
    if detail:
        extra.append(detail)
    if msg:
        extra.append(msg)
    if extra:
        print(f"        {dim}{' | '.join(extra)}{reset}")
sys.exit(0)
PY
  else
    step "${body}"
  fi
}

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml)

info "Starting Oracle + OpenMetadata..."
"${COMPOSE[@]}" up -d oracle openmetadata-mysql openmetadata-elasticsearch openmetadata-migrate openmetadata-server

info "Waiting for Oracle to become healthy..."
ATTEMPTS=60
for ((i = 1; i <= ATTEMPTS; i++)); do
  status="$(docker inspect --format='{{.State.Health.Status}}' camunda-tutorial-oracle 2>/dev/null || echo starting)"
  if [[ "$status" == "healthy" ]]; then
    ok "Oracle is healthy"
    break
  fi
  if ((i == ATTEMPTS)); then
    fail "Oracle did not become healthy in time (last status: ${status})."
    echo "Check: docker compose logs oracle" >&2
    exit 1
  fi
  if ((i % 6 == 0)); then
    dim "Oracle status: ${status} (attempt ${i}/${ATTEMPTS})..."
  fi
  sleep 5
done

info "Waiting for OpenMetadata API..."
OM_ATTEMPTS=90
for ((i = 1; i <= OM_ATTEMPTS; i++)); do
  om_code="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 http://localhost:8585/api/v1/system/version 2>/dev/null || true)"
  if [[ "$om_code" == "200" ]]; then
    ok "OpenMetadata is up (HTTP ${om_code})"
    break
  fi
  if ((i == OM_ATTEMPTS)); then
    warn "OpenMetadata not ready (last HTTP ${om_code}). Governance registry checks may fail until it is up."
    warn "Check: docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml logs openmetadata-server"
    break
  fi
  if ((i % 6 == 0)); then
    dim "OpenMetadata status http=${om_code} (attempt ${i}/${OM_ATTEMPTS})..."
  fi
  sleep 5
done

info "Checking service schemas..."
# Ensure registration_service exists (replaces legacy taxpayer_service on existing Oracle volumes).
"${COMPOSE[@]}" exec -T oracle bash -lc 'sqlplus -s sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba <<EOF
ALTER SESSION SET CONTAINER = FREEPDB1;
WHENEVER SQLERROR CONTINUE
CREATE USER registration_service IDENTIFIED BY "Registration_Pass_1"
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp QUOTA UNLIMITED ON users;
CREATE USER document_service IDENTIFIED BY "Document_Pass_1"
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO registration_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO document_service;
DROP TABLE registration_service.reg_ft_appl CASCADE CONSTRAINTS;
DROP TABLE refund_service.rfnd_appl CASCADE CONSTRAINTS;
ALTER TABLE registration_service.REG_TAXPAYER RENAME TO REGISTRATION_TAXPAYER;
ALTER TABLE registration_service.REGISTRATION_TAXPAYER RENAME CONSTRAINT UK_REG_TAXPAYER_ID TO UK_REGISTRATION_TAXPAYER_IDENTIFIER;
ALTER TABLE registration_service.REG_TAX_OBLIG RENAME TO REGISTRATION_TAX_OBLIGATION;
ALTER TABLE document_service.DOC_DOCUMENT RENAME TO DOCUMENT_RECORD;
ALTER TABLE document_service.DOC_DOCUMENT_EVENT RENAME TO DOCUMENT_EVENT;
ALTER TABLE governance_service.GOV_ASSET RENAME TO GOVERNANCE_ASSET;
ALTER TABLE governance_service.GOVERNANCE_ASSET RENAME CONSTRAINT UK_GOV_ASSET TO UK_GOVERNANCE_ASSET;
ALTER TABLE governance_service.GOV_VALIDATION RENAME TO GOVERNANCE_VALIDATION;
ALTER TABLE governance_service.GOVERNANCE_VALIDATION RENAME CONSTRAINT UK_GOV_VALIDATION_ID TO UK_GOVERNANCE_VALIDATION_ID;
UPDATE governance_service.GOVERNANCE_ASSET
   SET table_name = CASE table_name
         WHEN 'REG_TAXPAYER' THEN 'REGISTRATION_TAXPAYER'
         WHEN 'REG_TAX_OBLIG' THEN 'REGISTRATION_TAX_OBLIGATION'
         WHEN 'DOC_DOCUMENT' THEN 'DOCUMENT_RECORD'
         WHEN 'DOC_DOCUMENT_EVENT' THEN 'DOCUMENT_EVENT'
         WHEN 'GOV_ASSET' THEN 'GOVERNANCE_ASSET'
         WHEN 'GOV_VALIDATION' THEN 'GOVERNANCE_VALIDATION'
         ELSE table_name
       END
 WHERE table_name IN (
   'REG_TAXPAYER', 'REG_TAX_OBLIG', 'DOC_DOCUMENT', 'DOC_DOCUMENT_EVENT', 'GOV_ASSET', 'GOV_VALIDATION'
 );
DELETE FROM governance_service.GOVERNANCE_ASSET
 WHERE table_name IN ('RFND_APPL', 'REG_FT_APPL');
COMMIT;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER, CREATE TYPE TO camunda_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO governance_service;
GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO refund_service;
EXIT;
EOF' >/dev/null || true

if ! "${COMPOSE[@]}" exec -T oracle bash -lc \
  'sqlplus -s registration_service/Registration_Pass_1@//localhost:1521/FREEPDB1 <<EOF
WHENEVER SQLERROR EXIT FAILURE
SELECT 1 FROM dual;
EXIT;
EOF' >/dev/null 2>&1; then
  warn "registration_service schema not reachable — check Oracle logs"
else
  ok "Service schemas reachable"
fi

info "Building modules..."
"$MVN" -q -DskipTests package
ok "Maven package complete"

start_service() {
  local svc="$1"
  local pid_file="${PID_DIR}/${svc}.pid"
  local log_file="${LOG_DIR}/${svc}.log"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    ok "${svc} already running (pid $(cat "$pid_file"))"
    return 0
  fi

  rm -f "$pid_file"
  # Truncate log so "Started ..." detection refers to this run
  : >"$log_file"

  nohup "$MVN" -pl "services/${svc}" -am spring-boot:run \
    >"$log_file" 2>&1 &
  local launched=$!
  echo "$launched" >"$pid_file"

  # The mvn shell can exit after it execs/spawns the Maven JVM. Track that JVM,
  # which stays alive for the life of spring-boot:run.
  local attempt durable
  for ((attempt = 1; attempt <= 20; attempt++)); do
    durable="$(pgrep -f "services/${svc}.*spring-boot:run" 2>/dev/null | head -n 1 || true)"
    if [[ -n "$durable" ]]; then
      echo "$durable" >"$pid_file"
      break
    fi
    sleep 0.3
  done
  step "${svc} starting (pid $(cat "$pid_file"), log ${log_file})"
}

info "Starting governance-service first (catalog must be up before domain sync)..."
for svc in "${BOOTSTRAP_SERVICES[@]}"; do
  start_service "$svc"
done
wait_service "governance-service" "${GOVERNANCE_URL}/api/governance/naming-domains"

info "Starting document-service (processes create documents before other steps)..."
start_service "document-service"
wait_service "document-service" "http://localhost:8085/api/documents"

info "Starting authority-service (dummy NIRA and URSB APIs)..."
start_service "authority-service"
wait_service "authority-service" "http://localhost:8086/api/authorities/health"

info "Starting domain services..."
for svc in "${DOMAIN_SERVICES[@]}"; do
  start_service "$svc"
done

info "Waiting for domain services (Maven boot can take a few minutes)..."
wait_service "refund-service" "http://localhost:8082/api/refund-applications"
wait_service "registration-service" "http://localhost:8083/api/registrations/first-time"
# Catalog publish happens on domain-service ApplicationRunner after start;
# publisher now retries if governance was briefly unreachable.
sleep 3

CONFORM_FAILED=0
print_conformance "refund-service" || CONFORM_FAILED=1
print_conformance "registration-service" || CONFORM_FAILED=1
print_conformance "document-service" || CONFORM_FAILED=1

print_validations "refund-service" || true
print_validations "registration-service" || true
print_validations "document-service" || true

echo
info "Done. Endpoints:"
step "Camunda        http://localhost:8080  (demo/demo)"
step "OpenMetadata   http://localhost:8585  (admin/admin)"
step "Governance     ${GOVERNANCE_URL}/api/governance/tables"
step "Validations    ${GOVERNANCE_URL}/api/governance/validations"
step "  (sniffed BV) ${GOVERNANCE_URL}/api/governance/validations?service=refund-service"
step "Asset resolve  ${GOVERNANCE_URL}/api/governance/assets/refunds.application"
step "Conformance    ${GOVERNANCE_URL}/api/governance/conformance?service=refund-service"
step "OM webhook     ${GOVERNANCE_URL}/api/governance/webhooks/openmetadata"
step "Refunds        http://localhost:8082/api/refund-applications"
step "Registration   http://localhost:8083/api/registrations/first-time"
step "Documents      http://localhost:8085/api/documents"
step "NIRA           http://localhost:8086/api/nira/persons/lookup"
step "URSB           http://localhost:8086/api/ursb/entities/lookup"
echo

dim "Logs:  ${LOG_DIR}/"
dim "Stop:  scripts/stop-all.sh [--with-db] [--with-om]"

if [[ "$CONFORM_FAILED" -ne 0 ]]; then
  echo
  warn "one or more services are not governance-compliant (see above)."
  exit 2
fi

ok "All services governance-compliant"
