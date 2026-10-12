# Camunda microservices tutorial

## Layout

```text
camunda-tutorial/
├── db/oracle/                    # schema bootstrap SQL for Oracle 26ai
├── libs/
│   ├── governance-annotations/
│   ├── governance-runtime/
│   └── camunda-client/
└── services/
    ├── camunda-service/          # :8080 shared Camunda engine
    ├── governance-service/       # :8081 governance catalog
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

If you already created users and see `CREATE SESSION privilege` denied, fix grants:

```bash
docker compose exec oracle sqlplus -s sys/Oracle_Pass_1@//localhost:1521/FREE as sysdba @/opt/oracle/scripts/02-fix-session-grants.sql
```

Or copy/run `db/oracle/02-fix-session-grants.sql` / `01-create-schemas.sql` as SYSDBA on `FREE` (script switches to `FREEPDB1`).

Fresh volume (re-init schemas from scratch):

```bash
docker compose down -v
docker compose up -d oracle
```

## Runtime flow (domain APIs first)

1. Client calls `refund-service` or `taxpayer-service`
2. That service starts a process on **shared** `camunda-service`
3. Camunda delegates call `/internal/...` APIs on the domain services
4. Domain services own their schema/data and business logic

## Start everything

```bash
cd /Users/administrator/IdeaProjects/camunda-tutorial
./scripts/start-all.sh          # Oracle + all 4 services
./scripts/stop-all.sh           # stop services (DB keeps running)
./scripts/stop-all.sh --with-db # stop services + Oracle container
```

Logs/pids: `.logs/`. Or run mains in IntelliJ / one `mvn -pl services/<name> -am spring-boot:run` per terminal.

## URLs

| Service | URL |
|---------|-----|
| Camunda Cockpit | http://localhost:8080 (`demo`/`demo`) |
| Governance tables (metadata) | http://localhost:8081/api/governance/tables |
| Governance flat assets | http://localhost:8081/api/governance/assets |
| Refunds API | http://localhost:8082/api/refund-applications |
| Taxpayer API | http://localhost:8083/api/taxpayer-registrations |

- **Entity governance**: declare `@DataGovernance(owner, steward, domain, classification, retentionPolicy)` on every JPA entity.
- **Governance catalog** (`/api/governance/tables`): stores that metadata (+ field sensitivity).
- **Naming policy** (`/api/governance/naming-domains`): allow-listed domains + table prefixes (`REG_`, `RFND_`, …).
- **Oracle `COMMENT ON`**: only `@Comment` text (applied on domain-service startup).

Missing `@DataGovernance` fields, unknown domains, or bad table names fail `GET /api/governance/conformance?service=…`. Configure domains in `app.governance.naming`.
