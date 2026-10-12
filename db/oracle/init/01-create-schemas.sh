#!/bin/bash
# Runs inside gvenzl/oracle-free as part of /container-entrypoint-initdb.d
# createAppUser grants CONNECT/RESOURCE (incl. CREATE SESSION) in FREEPDB1.
set -euo pipefail

createAppUser camunda_service     "Camunda_Pass_1"
createAppUser governance_service  "Governance_Pass_1"
createAppUser refund_service      "Refund_Pass_1"
createAppUser taxpayer_service    "Taxpayer_Pass_1"

echo "Created microservice schemas in FREEPDB1."
