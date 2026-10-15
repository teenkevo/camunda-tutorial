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
    ├── refund-service/           # :8082 refund domain API + data
    └── taxpayer-service/         # :8083 taxpayer domain API + data
```

Each service uses layered packages: `controller`, `service`, `dto`, `entity`, `repository`, (+ `mapper` / `delegate` where needed).

## Database (Oracle 26ai)

Each microservice uses **Oracle** (`ojdbc17`) and its **own schema** (DB user). Defaults share one PDB (`FREEPDB1`) with four schemas; override `*_JDBC_URL` for a dedicated DB/PDB per service.

| Service | Schema (user) | Default password | JDBC URL env |
|---------|---------------|------------------|--------------|
| camunda-service | `CAMUNDA_SERVICE` | `Camunda_Pass_1` | `CAMUNDA_JDBC_URL` |
| governance-service | `GOVERNANCE_SERVICE` | `Governance_Pass_1` | `GOVERNANCE_JDBC_URL` |
| refund-service | `REFUND_SERVICE` | `Refund_Pass_1` | `REFUND_JDBC_URL` |
| taxpayer-service | `TAXPAYER_SERVICE` | `Taxpayer_Pass_1` | `TAXPAYER_JDBC_URL` |

Shared host default: `ORACLE_JDBC_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1`

### Start Oracle locally

```bash
docker compose up -d oracle
# first boot runs db/oracle/init/01-create-schemas.sh (createAppUser in FREEPDB1)
```

## Governance (domain + naming in code)

1. **Code** declares domain for naming: `@Domain("refunds")`.
2. **governance-service** syncs scanned entities/fields and runs naming conformance.
3. **OpenMetadata** remains available for optional registry attributes when an `assetId` is linked later.

Startup does **not** auto-seed OpenMetadata glossary terms (`app.governance.openmetadata.seed=false`). Set `seed: true` only if you want the demo seed runner.

```java
@Entity
@Table(name = "RFND_APPL")
@Domain("refunds")
public class RefundApplicationEntity { … }
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

1. Client calls `refund-service` or `taxpayer-service`
2. That service starts a process on **shared** `camunda-service`
3. Camunda delegates call `/internal/...` APIs on the domain services
4. Domain services own their schema/data and business logic

## Start everything

```bash
./scripts/start-all.sh          # Oracle + OpenMetadata + all 4 services
./scripts/stop-all.sh           # stop services (containers keep running)
./scripts/stop-all.sh --with-db # stop services + Oracle + OpenMetadata

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
| Taxpayer API | http://localhost:8083/api/taxpayer-registrations |

- **Naming policy** (`/api/governance/naming-domains`): allow-listed domains + table prefixes (`REG_`, `RFND_`, …) — still enforced in the application / governance-service.
- **Oracle `COMMENT ON`**: only `@Comment` text (applied on domain-service startup).

Missing `@Domain`, unknown domains, or bad table names fail  
`GET /api/governance/conformance?service=…`.
