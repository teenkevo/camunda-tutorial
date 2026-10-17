-- Create REGISTRATION_SERVICE schema on an existing FREEPDB1 (replaces TAXPAYER_SERVICE).
-- Run as SYSDBA, e.g.:
--   sqlplus sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba @db/oracle/02-registration-service-user.sql

ALTER SESSION SET CONTAINER = FREEPDB1;

WHENEVER SQLERROR CONTINUE
CREATE USER registration_service IDENTIFIED BY "Registration_Pass_1"
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp QUOTA UNLIMITED ON users;

GRANT CREATE SESSION, CONNECT, RESOURCE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TABLE, CREATE TRIGGER TO registration_service;

EXIT;
