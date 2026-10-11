# Camunda microservices tutorial

## Layout

```text
camunda-tutorial/
├── libs/
│   ├── governance-annotations/   # @Sensitive, @DataOwner, @DataDomain
│   ├── governance-runtime/       # PII masking + catalog publisher
│   └── camunda-client/           # shared CamundaProcessClient (start process)
└── services/
    ├── camunda-service/          # :8080 shared Camunda engine
    ├── governance-service/       # :8081 governance catalog
    ├── refund-service/           # :8082 refund domain API + data
    └── taxpayer-service/         # :8083 taxpayer domain API + data
```

Each service uses layered packages: `controller`, `service`, `dto`, `entity`, `repository`, (+ `mapper` / `client` / `delegate` where needed).

## Runtime flow (domain APIs first)

1. Client calls `refund-service` or `taxpayer-service`
2. That service starts a process on **shared** `camunda-service`
3. Camunda delegates call `/internal/...` APIs on the domain services
4. Domain services own the database and business logic

## Start order (4 terminals)

```bash
cd /Users/administrator/IdeaProjects/camunda-tutorial

mvn -pl services/camunda-service -am spring-boot:run
mvn -pl services/governance-service -am spring-boot:run
mvn -pl services/refund-service -am spring-boot:run
mvn -pl services/taxpayer-service -am spring-boot:run
```

Or run these mains in IntelliJ:
`CamundaServiceApplication`, `GovernanceServiceApplication`, `RefundServiceApplication`, `TaxpayerServiceApplication`.

## URLs

| Service | URL |
|---------|-----|
| Camunda Cockpit | http://localhost:8080 (`demo`/`demo`) |
| Governance catalog | http://localhost:8081/api/governance/assets |
| Refunds API | http://localhost:8082/api/refund-applications |
| Taxpayer API | http://localhost:8083/api/taxpayer-registrations |
