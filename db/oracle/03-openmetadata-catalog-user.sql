-- OpenMetadata Oracle connector catalog user (per OM docs).
-- Run as SYSDBA:
--   sqlplus sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba @db/oracle/03-openmetadata-catalog-user.sql
--
-- Use these credentials in OpenMetadata → Database Service → Oracle:
--   Username:            OM_CATALOG
--   Password:            Om_Catalog_Pass_1
--   Host and Port:       host.docker.internal:1521
--   Oracle Service Name: FREEPDB1
--   Database Schema:     REFUND_SERVICE   (or REGISTRATION_SERVICE / …)
--   Use DBA Tables:      on (SELECT_CATALOG_ROLE allows DBA_* reads)

ALTER SESSION SET CONTAINER = FREEPDB1;

WHENEVER SQLERROR CONTINUE

-- Role with session + dictionary access for metadata ingestion
CREATE ROLE om_catalog_role;
GRANT CREATE SESSION TO om_catalog_role;
GRANT SELECT_CATALOG_ROLE TO om_catalog_role;
-- Profiler / DQ + ALL_TABLES visibility across app schemas (tutorial)
GRANT SELECT ANY TABLE TO om_catalog_role;

CREATE USER om_catalog IDENTIFIED BY "Om_Catalog_Pass_1"
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp
  QUOTA UNLIMITED ON users;

GRANT om_catalog_role TO om_catalog;
ALTER USER om_catalog DEFAULT ROLE om_catalog_role;
ALTER USER om_catalog ACCOUNT UNLOCK;

-- Also allow dictionary reads if role enablement is restricted in some clients
GRANT SELECT_CATALOG_ROLE TO om_catalog;
GRANT SELECT ANY TABLE TO om_catalog;
GRANT CREATE SESSION TO om_catalog;

-- Explicit SELECT on known domain tables (safe if tables exist; CONTINUE if not yet created)
GRANT SELECT ON refund_service.REFUND_APPLICATION TO om_catalog;
GRANT SELECT ON refund_service.REFUND_APPLICATION_TAX_TYPE TO om_catalog;
GRANT SELECT ON document_service.DOCUMENT_RECORD TO om_catalog;
GRANT SELECT ON registration_service.REGISTRATION_TAXPAYER TO om_catalog;
GRANT SELECT ON registration_service.REGISTRATION_TAX_OBLIGATION TO om_catalog;
GRANT SELECT ON governance_service.GOVERNANCE_ASSET TO om_catalog;

PROMPT OpenMetadata catalog user OM_CATALOG ready.
EXIT;
