# Camunda Tutorial – Tax Processes with Sensitive Data Protection

A Spring Boot 3.5 + Camunda 7.24 application that runs two BPMN processes for a tax administration:

| Process | BPMN | REST entry point |
|---|---|---|
| Taxpayer registration | `src/main/resources/taxpayer-registration.bpmn` | `POST /api/taxpayer-registrations` |
| Refund application | `src/main/resources/refund-application.bpmn` | `POST /api/refund-applications`, `GET /api/refund-applications`, `GET /api/refund-applications/{applicationId}` |

The project also has a **sensitive data governance** layer (`com.example.camundatutorial.governance`). It classifies fields as PII, SENSITIVE or INTERNAL and masks them where the data leaves the trust boundary.

---

## Table of contents

1. [Getting started](#getting-started)
2. [Where masking applies](#where-masking-applies)
3. [Classifying data with `@Sensitive`](#classifying-data-with-sensitive)
4. [How protection is enforced](#how-protection-is-enforced)
5. [Configuration](#configuration)
6. [Current implementation status](#current-implementation-status)
7. [Checklist for new endpoints and clients](#checklist-for-new-endpoints-and-clients)
8. [Project layout](#project-layout)

---

## Getting started

**Prerequisites:** JDK 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

| URL | Purpose | Credentials |
|---|---|---|
| http://localhost:8080/camunda | Camunda Cockpit / Tasklist / Admin | `demo` / `demo` |
| http://localhost:8080/h2-console | In-memory H2 database | JDBC URL `jdbc:h2:mem:camunda-tutorial`, user `sa`, no password |

Example requests:

```bash
# Start a refund application
curl -X POST http://localhost:8080/api/refund-applications \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Jane","lastName":"Doe"}'

# List refund applications (PII fields come back masked)
curl http://localhost:8080/api/refund-applications
```

Run the tests:

```bash
mvn test
```

---

## Where masking applies

> **Rule:** PII and sensitive data is masked **only at external boundaries**.
> Communication between our own services is trusted, so data passes through **unmasked**.

| Communication channel | Mask PII / sensitive data? | Why |
|---|---|---|
| **Web portal → service** (responses sent back to the portal) | ✅ **Yes** | The browser is outside the trust boundary |
| **External clients → service** (third parties, partner systems, public API consumers) | ✅ **Yes** | The data leaves the organisation |
| **Outbound `WebClient` calls** to external parties | ✅ **Yes** | The payload leaves the trust boundary |
| **Outbound `RestClient` calls** to external parties | ✅ **Yes** | The payload leaves the trust boundary |
| **Service → service** (internal calls between our own microservices) | ❌ **No** | Internal services need the real values to do their work, for example matching a taxpayer by national ID |

### What the rule does *not* change

These protections are not about communication between parties, so they stay on no matter who the caller is:

| Control | Behaviour |
|---|---|
| **Logging** (`toString()` through `SensitivitySupport.sensitiveToString`) | Always masked. Logs are copied to many systems and people. |
| **DB column remarks** (`[PII]`, `[SENSITIVE]`, `[INTERNAL]`) | Always written. Governance tools such as OpenMetadata read these remarks. |
| **Entity response guard** (JPA `@Entity` must never be returned from a controller) | Always enforced. Controllers should map to DTOs for every caller. |
| **Camunda process variables** | Stored unmasked. The engine is internal, and the delegates need the real values. |

### Decision flow

```
                  Is the data leaving our trust boundary?
                                   │
              ┌────────────────────┴────────────────────┐
             YES                                       NO
 (web portal, external client,              (internal service-to-service
  outbound WebClient/RestClient               call, Camunda engine,
  call to an external party)                  internal persistence)
              │                                         │
   Apply protection policy                     Send real values
   (MASK or OMIT protected types)              (no masking)
```

---

## Classifying data with `@Sensitive`

Annotate the **field** (or its JavaBean getter) on entities **and** DTOs:

```java
@Sensitive(SensitiveType.PII)
private String firstName;
```

| `SensitiveType` | Meaning | Examples | Protected by default? |
|---|---|---|---|
| `PII` | Personally identifiable information | name, national ID, contact details, free-text comments about a person | ✅ Yes |
| `SENSITIVE` | Confidential business data that is not necessarily PII | internal risk scores, audit notes | ❌ No (opt in) |
| `INTERNAL` | Operational data not meant for external exposure | internal routing codes | ❌ No (opt in) |

Rules:

- Only fields and getters can be annotated. Class-level sensitivity is not supported.
- Annotate **both** the entity and the matching DTO field. The mapper and Jackson each check their own class, so either annotation is enough to trigger masking. Annotating both keeps the intent visible in both places.
- Override `toString()` on classes that contain sensitive fields so that logs stay masked:

  ```java
  @Override
  public String toString() {
      return SensitivitySupport.sensitiveToString(this);
  }
  ```

---

## How protection is enforced

The layers are listed in the order a value passes through them on its way out:

| # | Layer | Class | What it does |
|---|---|---|---|
| 1 | Database schema | `SensitiveColumnCommentIntegrator` | Writes `[PII]` / `[SENSITIVE]` / `[INTERNAL]` into the column comment so governance tools can find classified columns |
| 2 | Entity → DTO mapping | `SensitiveMappingSupport` (used by e.g. `RefundApplicationMapper`) | Copies same-named fields and replaces protected values with the mask (`String` fields) or `null` (OMIT mode, or non-`String` fields) |
| 3 | JSON serialization (safety net) | `SensitiveAnnotationIntrospector` + `SensitiveValueSerializer`, registered by `SensitivityJacksonConfiguration` | Masks or drops `@Sensitive` properties when Jackson writes JSON, even if a DTO was filled by hand |
| 4 | Controller guard | `EntityApiResponseGuard` | Returns HTTP 500 if a controller tries to return a JPA `@Entity` (or a collection/map/array of them) |
| 5 | Logging | `SensitiveToStringBuilder` via `SensitivitySupport` | Masks protected fields in `toString()` output |

Supporting classes:

- `SensitivityClassifier` finds `@Sensitive` on fields and getters and decides whether the configured threshold covers its type.
- `SensitiveDataMasker` is the single place that produces the mask value. It never turns `null` into a mask.

Example response with the default configuration:

```json
{
  "applicationId": "REF-1A2B3C4D",
  "firstName": "********",
  "lastName": "********",
  "station": "STATION-NORTH",
  "riskLevel": "low",
  "reviewComment": "********",
  "notificationMessage": "********",
  "decision": "APPROVED"
}
```

---

## Configuration

All settings live under `app.sensitivity.protection` in `application.yaml`:

```yaml
app:
  sensitivity:
    protection:
      enabled: true              # master switch for mapping, Jackson, logging and the entity guard
      response-mode: MASK        # MASK → replace with mask-value | OMIT → drop the property / map as null
      mask-value: "********"
      protected-types:           # threshold; add SENSITIVE and/or INTERNAL to widen it
        - PII
      block-entity-responses: true
```

| Property | Default | Notes |
|---|---|---|
| `enabled` | `true` | Setting `false` turns off **all** protection, logging included. Do not use it to make internal calls unmasked (see below). |
| `response-mode` | `MASK` | `MASK` keeps the property and shows the mask. `OMIT` removes it from JSON and maps it as `null`. |
| `mask-value` | `********` | Applied to any non-null protected value |
| `protected-types` | `[PII]` | Which `SensitiveType`s are masked |
| `block-entity-responses` | `true` | Turns `EntityApiResponseGuard` on or off |

---

## Current implementation status

> ⚠️ **The code applies protection everywhere today. It does not yet know which channel the data is going to.**
> The [rule above](#where-masking-applies) is the target behaviour. This section lists what has to change to get there.

| Area | Today | Target |
|---|---|---|
| Web portal / external client responses | Masked ✅ | Masked |
| Outbound `WebClient` / `RestClient` to external parties | Masked ✅ if the client is built from Spring Boot's auto-configured `WebClient.Builder` / `RestClient.Builder`, because they share the application `ObjectMapper` | Masked |
| Internal service-to-service responses | **Masked ❌** (same controllers, same `ObjectMapper`, same mapper) | Unmasked |
| Internal outbound `WebClient` / `RestClient` calls | **Masked ❌** (same shared `ObjectMapper`) | Unmasked |

Why internal traffic is masked today:

1. `SensitivityJacksonConfiguration` adds the masking introspector to the **application-wide** `ObjectMapper`. Spring MVC, `RestClient.Builder` and `WebClient.Builder` all reuse that mapper.
2. `SensitiveMappingSupport.outboundValue(...)` only checks the global `enabled` flag and the type threshold. It does not know who the caller is.

What is needed to close the gap (not implemented yet; the details still need a decision):

- **Tell callers apart.** Choose how an internal caller is recognised, for example a service-to-service auth scope or client credential, a separate internal API path such as `/internal/**`, or a separate port or gateway route.
- **Split the serialization.** Keep a masking `ObjectMapper` for external responses and external `WebClient` / `RestClient` instances, and give internal endpoints and internal clients a plain `ObjectMapper` without the introspector.
- **Make mapping depend on the audience.** Let `SensitiveMappingSupport` take the audience (internal or external), or provide separate mappers, so internal DTOs carry real values.
- **Keep logging and DB remarks unconditional.**

---

## Checklist for new endpoints and clients

When you add an **endpoint**:

- [ ] Return a DTO, never a JPA entity.
- [ ] Annotate sensitive DTO fields with `@Sensitive(...)`, matching the entity.
- [ ] Build the DTO with a mapper that uses `SensitiveMappingSupport`.
- [ ] Decide whether the endpoint serves the **web portal / external clients** (masked) or **internal services only** (unmasked).
- [ ] Override `toString()` with `SensitivitySupport.sensitiveToString(this)`.

When you add an outbound **`WebClient` / `RestClient`**:

- [ ] Decide whether the target is an **external party** (masked) or an **internal service** (unmasked).
- [ ] External targets: build the client from the auto-configured builder so it uses the masking `ObjectMapper`.
- [ ] Internal targets: once the split described above exists, use the internal (non-masking) configuration.

---

## Project layout

```
src/main/java/com/example/camundatutorial/
├── CamundaTutorialApplication.java
├── governance/          # @Sensitive, classification, masking, Jackson + Hibernate integration
├── refund/              # Refund application: controller, entity, DTOs, mapper, delegates
└── taxpayer/            # Taxpayer registration: controller, DTOs, delegates
src/main/resources/
├── application.yaml
├── refund-application.bpmn
├── taxpayer-registration.bpmn
└── META-INF/services/org.hibernate.integrator.spi.Integrator   # registers SensitiveColumnCommentIntegrator
src/test/java/com/example/camundatutorial/governance/
└── SensitivityProtectionTest.java
```
