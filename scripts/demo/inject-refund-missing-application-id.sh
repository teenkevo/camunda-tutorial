#!/usr/bin/env bash
# Demo: plant a Data Quality issue on REFUND_SERVICE.RFND_APPL —
# one row with APPLICATION_ID IS NULL (for OpenMetadata column Not-Null / null-proportion tests).
#
# Usage:
#   ./scripts/demo/inject-refund-missing-application-id.sh
#   ./scripts/demo/inject-refund-missing-application-id.sh --yes
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
source "${SCRIPT_DIR}/lib.sh"

require_docker

YES=0
for arg in "$@"; do
  case "$arg" in
    -y|--yes) YES=1 ;;
    -h|--help)
      cat <<'EOF'
Insert a RFND_APPL row with APPLICATION_ID NULL for DQ demos.

  ./scripts/demo/inject-refund-missing-application-id.sh
  ./scripts/demo/inject-refund-missing-application-id.sh --yes

Alters APPLICATION_ID to nullable if needed (demo only). Prints inspection SQL afterward.
EOF
      exit 0
      ;;
    *)
      fail "Unknown argument: $arg (try --help)"
      exit 1
      ;;
  esac
done

ORACLE_CTR="${ORACLE_CONTAINER:-camunda-tutorial-oracle}"
if ! docker inspect "$ORACLE_CTR" >/dev/null 2>&1; then
  fail "Oracle container '${ORACLE_CTR}' not found. Start it first (./scripts/start-all.sh)."
  exit 1
fi
if [[ "$(docker inspect -f '{{.State.Running}}' "$ORACLE_CTR" 2>/dev/null || echo false)" != "true" ]]; then
  fail "Oracle container '${ORACLE_CTR}' is not running. Start it: docker compose up -d oracle"
  exit 1
fi

info "Inject DQ issue: RFND_APPL row without APPLICATION_ID"
warn "This relaxes NOT NULL on APPLICATION_ID for the demo and inserts a bad row."

if [[ "$YES" -ne 1 ]]; then
  printf 'Continue? [y/N] '
  read -r answer
  case "$answer" in
    y|Y|yes|YES) ;;
    *)
      dim "Aborted."
      exit 0
      ;;
  esac
fi

sqlplus_refund() {
  docker exec -i "$ORACLE_CTR" bash -lc \
    'sqlplus -s refund_service/Refund_Pass_1@//localhost:1521/FREEPDB1'
}

info "Applying DDL + insert..."
sqlplus_refund <<'SQL'
SET FEEDBACK ON
-- Demo only: allow null APPLICATION_ID so DQ can fail on completeness.
-- ORA-01451 = column already nullable (safe to ignore on re-runs).
WHENEVER SQLERROR CONTINUE
ALTER TABLE RFND_APPL MODIFY (APPLICATION_ID NULL);
WHENEVER SQLERROR EXIT FAILURE

-- Avoid duplicate demo rows if script is re-run
DELETE FROM RFND_APPL WHERE STATUS = 'DQ_MISSING_APPLICATION_ID';
DELETE FROM RFND_APPL WHERE APPLICATION_ID IS NULL;

INSERT INTO RFND_APPL (
  ID, APPLICATION_ID, FIRST_NAME, LAST_NAME, STATUS, CREATED_AT, UPDATED_AT
) VALUES (
  (SELECT NVL(MAX(ID), 0) + 1 FROM RFND_APPL),
  NULL,
  'DQ',
  'TestWithoutAppId',
  'DQ_MISSING_APPLICATION_ID',
  SYSTIMESTAMP,
  SYSTIMESTAMP
);

COMMIT;

PROMPT --- Rows with missing APPLICATION_ID ---
COLUMN application_id FORMAT A20
COLUMN first_name FORMAT A12
COLUMN last_name FORMAT A20
COLUMN status FORMAT A30
SELECT ID, APPLICATION_ID, FIRST_NAME, LAST_NAME, STATUS
  FROM RFND_APPL
 WHERE APPLICATION_ID IS NULL
    OR STATUS = 'DQ_MISSING_APPLICATION_ID';

PROMPT --- Count ---
SELECT COUNT(*) AS missing_application_id_count
  FROM RFND_APPL
 WHERE APPLICATION_ID IS NULL;

EXIT;
SQL

ok "Bad row inserted (STATUS = DQ_MISSING_APPLICATION_ID)"

echo
info "Inspection SQL (run in sqlplus / SQL Developer / OM)"
cat <<'EOF'

-- As REFUND_SERVICE (or OM_CATALOG with SELECT):
SELECT ID, APPLICATION_ID, FIRST_NAME, LAST_NAME, STATUS
  FROM REFUND_SERVICE.RFND_APPL
 WHERE APPLICATION_ID IS NULL;

SELECT COUNT(*) AS missing_application_id_count
  FROM REFUND_SERVICE.RFND_APPL
 WHERE APPLICATION_ID IS NULL;

-- From host via Docker:
docker exec -it camunda-tutorial-oracle bash -lc \
  "sqlplus -s refund_service/Refund_Pass_1@//localhost:1521/FREEPDB1 <<'SQL'
SELECT ID, APPLICATION_ID, FIRST_NAME, LAST_NAME, STATUS
  FROM RFND_APPL WHERE APPLICATION_ID IS NULL;
EXIT;
SQL"

EOF

info "Cleanup SQL (after the DQ demo)"
cat <<'EOF'

DELETE FROM REFUND_SERVICE.RFND_APPL WHERE APPLICATION_ID IS NULL;
-- Restore NOT NULL if desired (fails if any nulls remain):
-- ALTER TABLE REFUND_SERVICE.RFND_APPL MODIFY (APPLICATION_ID VARCHAR2(255) NOT NULL);
COMMIT;


EOF

step "Re-run your OpenMetadata column test on APPLICATION_ID — it should fail."
dim "Companion: ./scripts/demo/reset-openmetadata.sh"
