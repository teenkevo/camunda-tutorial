#!/usr/bin/env bash
# Demo: plant a Data Quality issue on DOCUMENT_SERVICE.DOCUMENT_RECORD —
# one row with DRN IS NULL (for OpenMetadata column Not-Null / null-proportion tests).
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
Insert a DOCUMENT_RECORD row with DRN NULL for DQ demos.

  ./scripts/demo/inject-refund-missing-application-id.sh
  ./scripts/demo/inject-refund-missing-application-id.sh --yes

Alters DRN to nullable if needed (demo only). Prints inspection SQL afterward.
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

info "Inject DQ issue: DOCUMENT_RECORD row without DRN"
warn "This relaxes NOT NULL on DRN for the demo and inserts a bad row."

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

sqlplus_document() {
  docker exec -i "$ORACLE_CTR" bash -lc \
    'sqlplus -s document_service/Document_Pass_1@//localhost:1521/FREEPDB1'
}

info "Applying DDL + insert..."
sqlplus_document <<'SQL'
SET FEEDBACK ON
-- Demo only: allow null DRN so DQ can fail on completeness.
-- ORA-01451 = column already nullable (safe to ignore on re-runs).
WHENEVER SQLERROR CONTINUE
ALTER TABLE DOCUMENT_RECORD MODIFY (DRN NULL);
WHENEVER SQLERROR EXIT FAILURE

DELETE FROM DOCUMENT_RECORD WHERE STATUS = 'DQ_MISSING_DRN';
DELETE FROM DOCUMENT_RECORD WHERE DRN IS NULL;

INSERT INTO DOCUMENT_RECORD (
  ID, DRN, DOCUMENT_TYPE, SCHEMA_VERSION, STATUS, PAYLOAD, CREATED_AT, UPDATED_AT, VERSION_NO
) VALUES (
  (SELECT NVL(MAX(ID), 0) + 1 FROM DOCUMENT_RECORD),
  NULL,
  'REFUND_APPLICATION',
  1,
  'DQ_MISSING_DRN',
  '{}',
  SYSTIMESTAMP,
  SYSTIMESTAMP,
  0
);

COMMIT;

PROMPT --- Rows with missing DRN ---
SELECT ID, DRN, DOCUMENT_TYPE, STATUS FROM DOCUMENT_RECORD WHERE DRN IS NULL;

EXIT;
SQL

ok "Bad row inserted (STATUS = DQ_MISSING_DRN)"

echo
info "Inspection SQL (run in sqlplus / SQL Developer / OM)"
cat <<'EOF'

-- As DOCUMENT_SERVICE (or OM_CATALOG with SELECT):
SELECT ID, DRN, DOCUMENT_TYPE, STATUS
  FROM DOCUMENT_SERVICE.DOCUMENT_RECORD
 WHERE DRN IS NULL;

EOF

info "Cleanup SQL (after the DQ demo)"
cat <<'EOF'

DELETE FROM DOCUMENT_SERVICE.DOCUMENT_RECORD WHERE DRN IS NULL;
COMMIT;


EOF

step "Re-run your OpenMetadata column test on DRN — it should fail."
dim "Companion: ./scripts/demo/reset-openmetadata.sh"
