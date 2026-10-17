-- Drop application tables replaced by DOCUMENT_SERVICE.DOCUMENT_RECORD.
-- Run as SYSDBA against the CDB root service:
--   sqlplus sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba @db/oracle/04-drop-legacy-application-tables.sql

ALTER SESSION SET CONTAINER = FREEPDB1;
WHENEVER SQLERROR CONTINUE

DROP TABLE registration_service.reg_ft_appl CASCADE CONSTRAINTS;
DROP TABLE refund_service.rfnd_appl CASCADE CONSTRAINTS;
DELETE FROM governance_service.GOVERNANCE_ASSET
 WHERE table_name IN ('RFND_APPL', 'REG_FT_APPL');
COMMIT;

EXIT;
