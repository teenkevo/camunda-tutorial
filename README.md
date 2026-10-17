# Camunda microservices tutorial

## Layout

```text
camunda-tutorial/
├── db/oracle/                    # schema bootstrap SQL for Oracle 26ai
├── libs/
│   ├── governance-annotations/   # @Domain, @Sensitive, …
│   ├── governance-runtime/       # scan/publish, local metadata cache, refresh
│   └── camunda-client/
├── docker-compose.yml            # Oracle
├── docker-compose.openmetadata.yml  # MySQL + ES + OpenMetadata
└── services/
    ├── camunda-service/          # :8080 shared Camunda engine
    ├── governance-service/       # :8081 identity catalog + OpenMetadata facade
    ├── refund-service/           # :8082 refund rules + REFUND_APPLICATION
    ├── registration-service/     # :8083 registration rules + taxpayer master
    ├── document-service/         # :8085 documents, DRN, JSON payload
    └── authority-service/        # :8086 dummy NIRA and URSB APIs
```

`refund-service` uses flat layered packages (`controller`, `service`, `dto`, `entity`, `repository`).  
`registration-service` uses process packages (`process/firsttime/…`) plus `shared/` and `integration/` for cross-cutting domain and stubs.

## Database (Oracle 26ai)

Each microservice uses **Oracle** (`ojdbc17`) and its **own schema** (DB user). Defaults share one PDB (`FREEPDB1`) with four schemas; override `*_JDBC_URL` for a dedicated DB/PDB per service.

| Service | Schema (user) | Default password | JDBC URL env |
|---------|---------------|------------------|--------------|
| camunda-service | `CAMUNDA_SERVICE` | `Camunda_Pass_1` | `CAMUNDA_JDBC_URL` |
| governance-service | `GOVERNANCE_SERVICE` | `Governance_Pass_1` | `GOVERNANCE_JDBC_URL` |
| refund-service | `REFUND_SERVICE` | `Refund_Pass_1` | `REFUND_JDBC_URL` |
| registration-service | `REGISTRATION_SERVICE` | `Registration_Pass_1` | `REGISTRATION_JDBC_URL` |
| document-service | `DOCUMENT_SERVICE` | `Document_Pass_1` | `DOCUMENT_JDBC_URL` |

Shared host default: `ORACLE_JDBC_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1`

### Start Oracle locally

```bash
docker compose up -d oracle
# first boot runs db/oracle/init/01-create-schemas.sh (createAppUser in FREEPDB1)
# existing volumes: db/oracle/02-registration-service-user.sql (or start-all.sh)
```

## Governance (domain + naming in code)

1. **Code** declares domain for naming: `@Domain("refunds")` / `@Domain("taxpayer-registration")`.
2. **governance-service** syncs scanned entities/fields and runs naming conformance.
3. **OpenMetadata** remains available for optional registry attributes when an `assetId` is linked later.

Startup does **not** auto-seed OpenMetadata glossary terms (`app.governance.openmetadata.seed=false`). Set `seed: true` only if you want the demo seed runner.

```java
@Entity
@Table(name = "DOCUMENT_RECORD")
@Domain("documents")
public class DocumentEntity { … }
```

| Lives in code | Optional OpenMetadata |
|---------------|------------------------|
| `@Domain` (naming allow-list) | owner, steward |
| `@Table` name (naming check) | classification, retention |
| `@Sensitive` / `@Comment` | versioned updates |
| Bean Validation on `@Valid` request DTOs | (catalog only; OM test-case hook later) |

### Request validation catalog

Domain services discover Jakarta Bean Validation constraints on `@Valid @RequestBody` DTOs at startup and sync them to governance-service.

Stable id format: `{service}.{DtoSimpleName}.{propertyPath}.{ConstraintSimpleName}`  
Example: `refund-service.RefundApplicationRequest.firstName.NotBlank`

Browse: `GET /api/governance/validations?service=refund-service`

### OpenMetadata stack

```bash
docker compose -f docker-compose.yml -f docker-compose.openmetadata.yml up -d
# UI http://localhost:8585  — admin / admin
```

Prerequisites in compose: MySQL (`openmetadata/db`), Elasticsearch 8.10, migrate job, `openmetadata/server` (no Airflow).

Configure an OpenMetadata **generic alert** destination to  
`http://host.docker.internal:8081/api/governance/webhooks/openmetadata`  
so attribute changes fan out to service caches. Services also poll every 60s as a fallback.

### Governance update lifecycle

1. Admin updates owner for `refunds.application` in OpenMetadata (UI or API).
2. OM persists a new entity version.
3. Alert/webhook hits governance-service (or wait for poll).
4. Domain services `POST /internal/governance/refresh?assetId=…` and atomically replace cache when `latest.version >= current.version`.
5. New requests use updated rules — no redeploy.

## Runtime flow (domain APIs first)

1. Client calls `refund-service` or `registration-service`
2. That service starts a process on **shared** `camunda-service`
3. Camunda delegates call `/internal/...` APIs on the domain services
4. Domain services own their schema/data and business logic

### Documents and DRN

`document-service` stores every application document in `DOCUMENT_RECORD`. The first service task in each process is a create-document delegate. That call creates the document and a DRN (`DRN-REG-…` or `DRN-RFD-…`). Later steps use the DRN.

1. `POST /api/registrations/first-time/individual` or `.../non-individual`, or `POST /api/refund-applications`, starts the Camunda process
2. The create-document delegate writes the document and returns `drn`
3. Registration approval still creates `REGISTRATION_TAXPAYER` and `REGISTRATION_TAX_OBLIGATION`. A refund start writes `REFUND_APPLICATION` and `REFUND_APPLICATION_TAX_TYPE` in the refund schema, keyed by the same DRN.

### NIRA and URSB

`authority-service` is the dummy external boundary. An individual sends `nin` or `ain` plus `dateOfBirth` to `POST /api/registrations/first-time/individual/identity`, which calls NIRA. A non-individual sends `ern` plus `dateOfIncorporation` to `POST /api/registrations/first-time/non-individual/identity`, which calls URSB. When that record includes an email and telephone, the OTP is sent to those. When either contact is missing, the applicant supplies it at `.../individual/contacts` or `.../non-individual/contacts`. `POST /api/registrations/first-time/individual` or `.../non-individual` then checks the OTP and starts the process.

Known records (any other identifier of at least 5 characters is generated with contacts; a known number with the wrong date returns 404):

| Authority | Identifier | Date | Name | Contacts |
|-----------|------------|------|------|----------|
| NIRA | `CM90001234` | `1990-05-12` | Amina Nakato | email and telephone |
| NIRA | `AIN00012345` | `1988-11-02` | John Okello | email and telephone |
| NIRA | `CM90008888` | `1985-07-01` | Peter Mukasa | none |
| URSB | `80020001234567` | `2018-03-20` | Nile Harvest Limited | email and telephone |
| URSB | `80020007654321` | `2012-09-01` | Kampala Traders Cooperative | email and telephone |
| URSB | `80020000000001` | `2010-01-01` | Silent Books Limited | none |

## Start everything

```bash
./scripts/start-all.sh          # Oracle + OpenMetadata + all 4 services
./scripts/stop-all.sh           # stop services (Oracle and OpenMetadata keep running)
./scripts/stop-all.sh --with-db # also stop Oracle
./scripts/stop-all.sh --with-om # also stop OpenMetadata

# Demo helpers (see scripts/demo/)
./scripts/demo/reset-openmetadata.sh --yes   # wipe OM catalog, keep Oracle
```

Or run mains in IntelliJ / one `mvn -pl services/<name> -am spring-boot:run` per terminal.

## URLs

| Service | URL |
|---------|-----|
| Camunda Cockpit | http://localhost:8080 (`demo`/`demo`) |
| OpenMetadata | http://localhost:8585 (`admin`/`admin`) |
| Governance tables | http://localhost:8081/api/governance/tables |
| Validation catalog | http://localhost:8081/api/governance/validations |
| Resolve asset | http://localhost:8081/api/governance/assets/refunds.application |
| Conformance | http://localhost:8081/api/governance/conformance?service=refund-service |
| OM webhook | http://localhost:8081/api/governance/webhooks/openmetadata |
| Refunds API | http://localhost:8082/api/refund-applications |
| Individual registration | http://localhost:8083/api/registrations/first-time/individual |
| Non-individual registration | http://localhost:8083/api/registrations/first-time/non-individual |
| Documents API | http://localhost:8085/api/documents |
| NIRA lookup | http://localhost:8086/api/nira/persons/lookup |
| URSB lookup | http://localhost:8086/api/ursb/entities/lookup |

- **Naming policy** (`/api/governance/naming-domains`): allow-listed domains + full-word table prefixes (`REGISTRATION_`, `REFUND_`, `DOCUMENT_`, `GOVERNANCE_`) — still enforced in the application / governance-service.
- **Oracle `COMMENT ON`**: only `@Comment` text (applied on domain-service startup).

Missing `@Domain`, unknown domains, or bad table names fail  
`GET /api/governance/conformance?service=…`.
