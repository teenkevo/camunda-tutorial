#!/bin/bash
# Runs inside gvenzl/oracle-free as part of /container-entrypoint-initdb.d
# createAppUser grants CONNECT/RESOURCE (incl. CREATE SESSION) in FREEPDB1.
set -euo pipefail

createAppUser camunda_service     "Camunda_Pass_1"
createAppUser governance_service  "Governance_Pass_1"
createAppUser refund_service      "Refund_Pass_1"
createAppUser registration_service "Registration_Pass_1"
createAppUser document_service     "Document_Pass_1"

# OpenMetadata ingestion user (CREATE SESSION + SELECT_CATALOG_ROLE + SELECT)
# Full grants also live in db/oracle/03-openmetadata-catalog-user.sql for existing DBs.
sqlplus -s / as sysdba <<'SQL'
ALTER SESSION SET CONTAINER = FREEPDB1;
WHENEVER SQLERROR CONTINUE
CREATE ROLE om_catalog_role;
GRANT CREATE SESSION TO om_catalog_role;
GRANT SELECT_CATALOG_ROLE TO om_catalog_role;
GRANT SELECT ANY TABLE TO om_catalog_role;
CREATE USER om_catalog IDENTIFIED BY "Om_Catalog_Pass_1"
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp QUOTA UNLIMITED ON users;
GRANT om_catalog_role TO om_catalog;
ALTER USER om_catalog DEFAULT ROLE om_catalog_role;
GRANT SELECT_CATALOG_ROLE TO om_catalog;
GRANT SELECT ANY TABLE TO om_catalog;
GRANT CREATE SESSION TO om_catalog;
EXIT;
SQL

echo "Created microservice schemas + OM_CATALOG in FREEPDB1."
