# Data Flow Diagram — First-Time Taxpayer Registration (Level 0 → Level 3)

Scope: the first-time registration application exposed by `registration-service`
(`/api/registrations/first-time/**`) and orchestrated by the Camunda process
`reg-first-time-taxpayer` in `camunda-service`, persisting through `document-service`.

## Notation

| Shape | Meaning |
|---|---|
| Rectangle | External entity (source / sink outside the system boundary) |
| Rounded box | Process (numbered `n.0`, `n.m`, `n.m.k` by level) |
| Cylinder | Data store (`Dn`) |
| Arrow label | Data flow (what moves, not control) |

## Data dictionary — stores

| ID | Store | Owner | Physical |
|---|---|---|---|
| D1 | Registration document | document-service | `DOCUMENT_RECORD` (DRN, status, processInstanceId, taxpayerId, JSON payload) |
| D2 | Document event log | document-service | `DOCUMENT_EVENT` (CREATED, IDENTITY_VALIDATED, TAX_TYPES_PREPARED, APPLICATION_VALIDATED, TAX_TYPE_RECORDED, APPROVED, REJECTED) |
| D3 | Taxpayer register | registration-service | `REGISTRATION_TAXPAYER` |
| D4 | Tax obligations | registration-service | `REGISTRATION_TAX_OBLIGATION` |
| D5 | Process state | camunda-service | Camunda `ACT_*` tables (process variables, user tasks) |
| D6 | Issued OTPs | registration-service | In-memory set keyed `email\|telephone` (`StubOtpService`) |
| D7 | Audit trail | registration / document services | `REGISTRATION_AUDIT`, `DOCUMENT_AUDIT` (written by `AuditTrailListener` on every insert/update of D1–D4) |

## Data dictionary — external entities

| ID | Entity | Interface |
|---|---|---|
| E1 | Taxpayer applicant (individual or non-individual) | Portal → `POST /api/registrations/first-time/{individual\|non-individual}[/identity\|/contacts]` |
| E2 | NIRA (national ID registry) | authority-service `POST /api/nira/persons/lookup` |
| E3 | URSB (business registry) | authority-service URSB lookup |
| E4 | Registration Officer | Camunda Tasklist user task `ReviewRegistration` |
| E5 | Email / SMS channel | OTP delivery (stubbed) |

---

## Level 0 — Context diagram

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext
    E4["E4 Registration Officer"]:::ext
    E5["E5 Email / SMS channel"]:::ext

    P0("0 First-Time Taxpayer<br/>Registration System"):::proc

    E1 -- "NIN/AIN + DOB or ERN + date of incorporation,<br/>missing email/telephone, OTP code, tax types" --> P0
    P0 -- "identity prefill (name, contacts, flags),<br/>DRN + processInstanceId, application status" --> E1
    P0 -- "NIN/AIN, DOB, identifierType" --> E2
    E2 -- "person record (names, DOB, gender,<br/>nationality, email, telephone)" --> P0
    P0 -- "ERN, registration date" --> E3
    E3 -- "entity record (name, type, status,<br/>email, telephone)" --> P0
    P0 -- "review task (applicant, tax types, validation)" --> E4
    E4 -- "approved flag, review comment" --> P0
    P0 -- "OTP to email + telephone" --> E5
```

---

## Level 1 — System decomposition

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext
    E4["E4 Registration Officer"]:::ext
    E5["E5 Email / SMS"]:::ext

    P1("1.0 Verify identity<br/>and eligibility"):::proc
    P2("2.0 Verify OTP and<br/>submit application"):::proc
    P3("3.0 Process application<br/>(automated BPMN steps)"):::proc
    P4("4.0 Officer review"):::proc
    P5("5.0 Finalise outcome"):::proc
    P6("6.0 Query applications"):::proc

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D3[("D3 Taxpayer register")]:::store
    D4[("D4 Tax obligations")]:::store
    D5[("D5 Process state")]:::store
    D6[("D6 Issued OTPs")]:::store
    D7[("D7 Audit trail")]:::store

    E1 -- "identifier, DOB/incorp date,<br/>optional email/telephone" --> P1
    P1 -- "identifier + date" --> E2
    P1 -- "identifier + date" --> E3
    E2 -- "person record" --> P1
    E3 -- "entity record" --> P1
    D3 -- "lifecycle state for identifier" --> P1
    P1 -- "OTP" --> E5
    P1 -- "issued OTP key" --> D6
    P1 -- "identity response (name, contacts,<br/>contactsRequired, otpSent)" --> E1

    E1 -- "identifier, date, contacts,<br/>OTP code, tax types" --> P2
    D6 -- "issued OTP key" --> P2
    P2 -. "re-verification (via 1.0)" .-> P1
    P2 -- "process variables" --> D5
    P2 -- "resolved registration" --> P3
    D1 -- "DRN for process instance" --> P2
    P2 -- "DRN, processInstanceId, message" --> E1

    D5 -- "process variables" --> P3
    P3 -- "identifier + date" --> E2
    P3 -- "identifier + date" --> E3
    E2 -- "person record" --> P3
    E3 -- "entity record" --> P3
    D3 -- "lifecycle state" --> P3
    P3 -- "new document, payload patches" --> D1
    D1 -- "document + payload" --> P3
    P3 -- "events" --> D2
    P3 -- "drn, identityMatched, valid,<br/>registeredTaxTypes" --> D5
    P3 -- "validated application" --> P4
    P3 -- "identity failed / invalid" --> P5

    D5 -- "review task" --> P4
    P4 -- "review task" --> E4
    E4 -- "approved, reviewComment" --> P4
    P4 -- "decision" --> D5
    P4 -- "decision" --> P5

    D1 -- "document + payload" --> P5
    P5 -- "ACTIVE taxpayer" --> D3
    P5 -- "ACTIVE obligations" --> D4
    P5 -- "status APPROVED/REJECTED,<br/>taxpayerId" --> D1
    P5 -- "APPROVED/REJECTED event" --> D2
    P5 -- "registrationStatus, taxpayerId" --> D5

    E1 -- "DRN or list request" --> P6
    D1 -- "registration documents" --> P6
    P6 -- "application status view" --> E1

    D1 -. "row changes" .-> D7
    D3 -. "row changes" .-> D7
    D4 -. "row changes" .-> D7
```

---

## Level 2 — Process decompositions

### 1.0 Verify identity and eligibility

Endpoints: `POST .../individual/identity`, `.../individual/contacts`,
`.../non-individual/identity`, `.../non-individual/contacts`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext
    E5["E5 Email / SMS"]:::ext
    D3[("D3 Taxpayer register")]:::store
    D6[("D6 Issued OTPs")]:::store

    P11("1.1 Normalise<br/>identity request"):::proc
    P12("1.2 Look up identity<br/>at authority"):::proc
    P13("1.3 Check for existing<br/>active registration"):::proc
    P14("1.4 Resolve contact<br/>channels"):::proc
    P15("1.5 Issue OTP"):::proc
    P16("1.6 Build identity<br/>response"):::proc

    E1 -- "nin | ain + dateOfBirth,<br/>or ern + dateOfIncorporation,<br/>[email, telephone]" --> P11
    P11 -- "identifierType, identifierValue (trimmed, upper-case),<br/>dateOfBirthOrIncorp" --> P12
    P12 -- "identifier + date" --> E2
    P12 -- "identifier + date" --> E3
    E2 -- "person record" --> P12
    E3 -- "entity record" --> P12
    P12 -- "IdentityPrefill (authority, displayName,<br/>email, telephone)" --> P13
    D3 -- "lifecycleState by identifier" --> P13
    P13 -- "eligible prefill" --> P14
    P11 -- "supplied email, telephone" --> P14
    P14 -- "ContactChannels (email, telephone,<br/>fromAuthority flags, complete)" --> P15
    P14 -- "ContactChannels" --> P16
    P15 -- "OTP" --> E5
    P15 -- "key email|telephone" --> D6
    P15 -- "otpSent" --> P16
    P16 -- "identity response" --> E1
    P12 -- "404 not matched" --> E1
    P13 -- "409 already active" --> E1
    P14 -- "400 missing / invalid contacts<br/>(contacts endpoint only)" --> E1
```

### 2.0 Verify OTP and submit application

Endpoints: `POST .../individual`, `POST .../non-individual`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    D1[("D1 Registration document")]:::store
    D5[("D5 Process state")]:::store
    D6[("D6 Issued OTPs")]:::store

    P21("2.1 Re-verify identity,<br/>eligibility, contacts<br/>(1.1–1.4)"):::proc
    P22("2.2 Verify OTP"):::proc
    P23("2.3 Normalise and<br/>validate tax types"):::proc
    P24("2.4 Build resolved<br/>registration"):::proc
    P25("2.5 Start process<br/>instance"):::proc
    P26("2.6 Resolve DRN and<br/>respond"):::proc

    E1 -- "identifier, date, email, telephone,<br/>otpCode, taxTypes" --> P21
    P21 -- "prefill + complete ContactChannels" --> P22
    E1 -- "otpCode" --> P22
    D6 -- "issued key" --> P22
    P22 -- "OTP verified" --> P23
    E1 -- "raw taxTypes" --> P23
    P23 -- "normalised taxTypes" --> P24
    P21 -- "displayName, email, telephone" --> P24
    P24 -- "ResolvedRegistration" --> P25
    P25 -- "variables: identifierType, identifierValue,<br/>dateOfBirthOrIncorp, email, telephone,<br/>taxpayerName, taxTypes, taxTypesInput" --> D5
    P25 -- "processInstanceId" --> P26
    D1 -- "document by processInstanceId (drn)" --> P26
    P26 -- "201: drn, processInstanceId,<br/>processDefinitionKey, taxTypes, message" --> E1
    P22 -- "400 OTP verification failed" --> E1
    P23 -- "400 empty / unsupported tax types" --> E1
```

### 3.0 Process application (automated BPMN steps)

Camunda delegates in `camunda-service` → `registration-service` `/internal/reg-first-time/*`
and `document-service` `/internal/documents`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext
    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D3[("D3 Taxpayer register")]:::store
    D5[("D5 Process state")]:::store

    P31("3.1 Create registration<br/>document"):::proc
    P32("3.2 Validate identity<br/>with NIRA / URSB"):::proc
    P33("3.3 Prepare tax<br/>type list"):::proc
    P34("3.4 Validate<br/>application"):::proc
    P35("3.5 Register tax type<br/>(sequential, per tax type)"):::proc
    P4("4.0 Officer review"):::proc
    P52("5.2 Reject application"):::proc

    D5 -- "applicant variables" --> P31
    P31 -- "documentType, processInstanceId,<br/>createdBy, payload (otpVerified=true)" --> D1
    P31 -- "CREATED" --> D2
    P31 -- "drn, documentId, documentStatus" --> D5

    D5 -- "drn + base body" --> P32
    D1 -- "payload identifier + date" --> P32
    P32 -- "identifier + date" --> E2
    P32 -- "identifier + date" --> E3
    E2 -- "person record" --> P32
    E3 -- "entity record" --> P32
    P32 -- "identity patch" --> D1
    P32 -- "IDENTITY_VALIDATED" --> D2
    P32 -- "identityMatched, valid, validationMessage,<br/>taxpayerName, email, telephone" --> D5
    P32 -- "identityMatched = false" --> P52

    D5 -- "taxTypesInput, taxTypes" --> P33
    D1 -- "payload taxTypes" --> P33
    P33 -- "taxTypes, registeredTaxTypes = []" --> D1
    P33 -- "TAX_TYPES_PREPARED" --> D2

    D1 -- "payload" --> P34
    D3 -- "lifecycleState" --> P34
    P34 -- "validationMessage" --> D1
    P34 -- "APPLICATION_VALIDATED" --> D2
    P34 -- "valid" --> D5
    P34 -- "valid = false" --> P52

    D5 -- "taxType (loop element)" --> P35
    D1 -- "registeredTaxTypes" --> P35
    P35 -- "registeredTaxTypes + taxType" --> D1
    P35 -- "TAX_TYPE_RECORDED" --> D2
    P35 -- "registeredTaxTypes, lastRegistrationRef" --> D5
    P35 -- "application ready for review" --> P4
```

### 4.0 Officer review (primitive at Level 2)

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E4["E4 Registration Officer"]:::ext
    D5[("D5 Process state")]:::store

    P41("4.1 Present review task<br/>(assignee demo)"):::proc
    P42("4.2 Capture decision"):::proc
    P51("5.1 Activate taxpayer"):::proc
    P52("5.2 Reject application"):::proc

    D5 -- "process variables (applicant, taxTypes,<br/>validationMessage, drn)" --> P41
    P41 -- "task form" --> E4
    E4 -- "approved (boolean), reviewComment" --> P42
    P42 -- "approved, reviewComment" --> D5
    P42 -- "approved = true" --> P51
    P42 -- "approved = false" --> P52
```

### 5.0 Finalise outcome

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D3[("D3 Taxpayer register")]:::store
    D4[("D4 Tax obligations")]:::store
    D5[("D5 Process state")]:::store

    P51("5.1 Activate taxpayer<br/>and obligations"):::proc
    P52("5.2 Mark application<br/>rejected"):::proc

    D5 -- "drn" --> P51
    D1 -- "status, taxpayerId, payload" --> P51
    P51 -- "taxpayer (ACTIVE)" --> D3
    D3 -- "taxpayer id" --> P51
    D4 -- "existing obligation types" --> P51
    P51 -- "new obligations (ACTIVE, registrationRef)" --> D4
    P51 -- "status APPROVED, taxpayerId,<br/>approved = true" --> D1
    P51 -- "APPROVED" --> D2
    P51 -- "registrationStatus = ACTIVE, taxpayerId" --> D5

    D5 -- "drn, reviewComment" --> P52
    D1 -- "status" --> P52
    P52 -- "status REJECTED, approved = false,<br/>reviewComment" --> D1
    P52 -- "REJECTED" --> D2
    P52 -- "registrationStatus = REJECTED" --> D5
```

### 6.0 Query applications (primitive at Level 2)

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    D1[("D1 Registration document")]:::store

    P61("6.1 List applications<br/>(INDIVIDUAL_ / NON_INDIVIDUAL_REGISTRATION)"):::proc
    P62("6.2 Get application by DRN"):::proc
    P63("6.3 Map document to<br/>application response"):::proc

    E1 -- "GET /first-time" --> P61
    E1 -- "GET /first-time/{drn}" --> P62
    D1 -- "documents by type" --> P61
    D1 -- "document by DRN" --> P62
    P61 -- "documents" --> P63
    P62 -- "document" --> P63
    P63 -- "drn, status, identity, contacts, taxTypes,<br/>registeredTaxTypes, approved, timestamps" --> E1
```

---

## Level 3 — Primitive process decompositions

### 1.2 Look up identity at authority

`IdentityLookup.lookup`, `NiraClient`, `UrsbClient`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext

    P121("1.2.1 Reject SYS identifier<br/>and blank inputs"):::proc
    P122("1.2.2 Route by<br/>identifier type"):::proc
    P123("1.2.3 Query NIRA person"):::proc
    P124("1.2.4 Query URSB entity"):::proc
    P125("1.2.5 Map NIRA person<br/>to prefill"):::proc
    P126("1.2.6 Map URSB entity<br/>to prefill"):::proc
    P127("1.2.7 Enforce match"):::proc
    P13("1.3 Check existing<br/>registration"):::proc

    P121 -- "400 SYS not allowed" --> E1
    P121 -- "identifierType, value, date" --> P122
    P122 -- "NIN / AIN, dateOfBirth" --> P123
    P122 -- "ERN, registrationDate" --> P124
    P123 -- "nationalIdentificationNumber,<br/>dateOfBirth, identifierType" --> E2
    E2 -- "NiraPerson | 404" --> P123
    P124 -- "registrationNumber, registrationDate" --> E3
    E3 -- "UrsbEntity | 404" --> P124
    P123 -- "NiraPerson" --> P125
    P124 -- "UrsbEntity" --> P126
    P125 -- "displayName = givenNames + surname,<br/>email, telephone, identity map" --> P127
    P126 -- "displayName = entityName,<br/>email, telephone, identity map" --> P127
    P123 -- "unmatched (NIRA)" --> P127
    P124 -- "unmatched (URSB)" --> P127
    P127 -- "404 + authority message" --> E1
    P127 -- "matched IdentityPrefill" --> P13
```

### 1.4 Resolve contact channels

`FirstTimeRegistrationService.channels`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000

    E1["E1 Taxpayer applicant"]:::ext

    P141("1.4.1 Take authority<br/>email / telephone"):::proc
    P142("1.4.2 Fall back to supplied<br/>email and validate"):::proc
    P143("1.4.3 Fall back to supplied<br/>telephone"):::proc
    P144("1.4.4 Assess completeness"):::proc
    P15("1.5 Issue OTP"):::proc
    P16("1.6 Build response"):::proc

    P141 -- "authority email (if blank)" --> P142
    P141 -- "authority telephone (if blank)" --> P143
    E1 -- "supplied email" --> P142
    E1 -- "supplied telephone" --> P143
    P142 -- "400 email not valid" --> E1
    P141 -- "emailFromAuthority,<br/>telephoneFromAuthority" --> P144
    P142 -- "email" --> P144
    P143 -- "telephone" --> P144
    P144 -- "complete = true" --> P15
    P144 -- "contactsRequired,<br/>missing-contacts message" --> P16
```

### 2.2 Verify OTP

`StubOtpService.verify`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    D6[("D6 Issued OTPs")]:::store

    P221("2.2.1 Build key<br/>lower(email)|telephone"):::proc
    P222("2.2.2 Confirm OTP<br/>was issued"):::proc
    P223("2.2.3 Check code"):::proc
    P23("2.3 Normalise<br/>tax types"):::proc

    P221 -- "key" --> P222
    D6 -- "issued keys" --> P222
    P222 -- "issued" --> P223
    E1 -- "otpCode" --> P223
    P222 -- "400 OTP verification failed" --> E1
    P223 -- "400 OTP verification failed" --> E1
    P223 -- "verified" --> P23
```

### 2.3 Normalise and validate tax types

`normalizeTaxTypes`, `validateSupportedTaxTypes` (also used by 3.3).

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000

    E1["E1 Taxpayer applicant"]:::ext

    P231("2.3.1 Split raw input<br/>(list or comma string)"):::proc
    P232("2.3.2 Trim, upper-case,<br/>INCOME → INCOME_TAX"):::proc
    P233("2.3.3 De-duplicate,<br/>keep order"):::proc
    P234("2.3.4 Require at least one"):::proc
    P235("2.3.5 Check supported set<br/>INCOME_TAX, VAT, PAYE, WHT, RENTAL,<br/>LED, GAMING, STAMP_DUTY, CIT"):::proc
    P24("2.4 Build resolved<br/>registration"):::proc

    E1 -- "taxTypes" --> P231
    P231 -- "values" --> P232
    P232 -- "codes" --> P233
    P233 -- "taxTypes" --> P234
    P234 -- "400 at least one tax type" --> E1
    P234 -- "taxTypes" --> P235
    P235 -- "400 unsupported tax types" --> E1
    P235 -- "validated taxTypes" --> P24
```

### 2.5 / 2.6 Start process instance and resolve DRN

`FirstTimeRegistrationController.start`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E1["E1 Taxpayer applicant"]:::ext
    D1[("D1 Registration document")]:::store
    D5[("D5 Process state")]:::store

    P251("2.5.1 Map ResolvedRegistration<br/>to process variables"):::proc
    P252("2.5.2 Start reg-first-time-taxpayer"):::proc
    P31("3.1 Create registration<br/>document"):::proc
    P261("2.6.1 Fetch document by<br/>processInstanceId"):::proc
    P262("2.6.2 Build start response"):::proc

    P251 -- "variables (taxTypesInput = joined list)" --> P252
    P252 -- "process instance + variables" --> D5
    P252 -. "synchronous first step" .-> P31
    P252 -- "processInstanceId" --> P261
    D1 -- "drn" --> P261
    P261 -- "drn, processInstanceId" --> P262
    P262 -- "201 start response" --> E1
```

### 3.1 Create registration document

`RegCreateDocumentDelegate` → `DocumentService.create`.

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D5[("D5 Process state")]:::store
    D7[("D7 Audit trail")]:::store

    P311("3.1.1 Assemble payload<br/>from process variables"):::proc
    P312("3.1.2 Derive document type<br/>ERN → NON_INDIVIDUAL_REGISTRATION<br/>else INDIVIDUAL_REGISTRATION"):::proc
    P313("3.1.3 Idempotency check<br/>by processInstanceId + type"):::proc
    P314("3.1.4 Generate DRN<br/>DRN-REG-xxxxxxxx"):::proc
    P315("3.1.5 Insert document<br/>status IN_PROGRESS"):::proc
    P316("3.1.6 Record CREATED event"):::proc
    P317("3.1.7 Store document refs<br/>in process"):::proc

    D5 -- "identifierType, identifierValue, dateOfBirthOrIncorp,<br/>email, telephone, taxpayerName, taxTypes" --> P311
    P311 -- "payload (registeredTaxTypes = [], otpVerified = true)" --> P312
    P312 -- "documentType, processInstanceId,<br/>createdBy, payload" --> P313
    D1 -- "existing document" --> P313
    P313 -- "existing document (reuse)" --> P317
    P313 -- "new request" --> P314
    P314 -- "drn" --> P315
    P315 -- "document row" --> D1
    D1 -. "row change" .-> D7
    P315 -- "document" --> P316
    P316 -- "CREATED, toStatus IN_PROGRESS" --> D2
    P315 -- "drn, id, status" --> P317
    P317 -- "drn, documentId, documentStatus" --> D5
```

### 3.2 Validate identity with NIRA / URSB

`RegValidateIdentityDelegate` → `FirstTimeRegistrationService.validateIdentity`.

```mermaid
flowchart LR
    classDef ext fill:#fde7c8,stroke:#b36b00,color:#000
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    E2["E2 NIRA"]:::ext
    E3["E3 URSB"]:::ext
    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D5[("D5 Process state")]:::store

    P321("3.2.1 Load document by DRN"):::proc
    P322("3.2.2 Look up authority<br/>(same as 1.2)"):::proc
    P323("3.2.3 Build identity patch,<br/>fill blank name / contacts"):::proc
    P324("3.2.4 Merge patch into<br/>document payload"):::proc
    P325("3.2.5 Return step result<br/>to process"):::proc

    D5 -- "drn" --> P321
    D1 -- "document + payload" --> P321
    P321 -- "identifierType, value, date" --> P322
    P322 -- "identifier + date" --> E2
    P322 -- "identifier + date" --> E3
    E2 -- "person record" --> P322
    E3 -- "entity record" --> P322
    P322 -- "IdentityPrefill" --> P323
    P321 -- "existing taxpayerName, email, telephone" --> P323
    P323 -- "identityMatched, identityAuthority,<br/>validationMessage, identity, [name, contacts]" --> P324
    P324 -- "merged payload" --> D1
    P324 -- "IDENTITY_VALIDATED" --> D2
    P324 -- "updated payload" --> P325
    P325 -- "identityMatched, valid, validationMessage,<br/>taxpayerName, email, telephone" --> D5
```

### 3.3 Prepare tax type list

`RegPrepareTaxTypesDelegate` → `FirstTimeRegistrationService.prepareTaxTypes`.

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D5[("D5 Process state")]:::store

    P331("3.3.1 Select source<br/>taxTypesInput → taxTypes → payload"):::proc
    P332("3.3.2 Normalise and validate<br/>(same as 2.3)"):::proc
    P333("3.3.3 Reset registeredTaxTypes<br/>and merge"):::proc

    D5 -- "taxTypesInput, taxTypes" --> P331
    D1 -- "payload taxTypes" --> P331
    P331 -- "raw tax types" --> P332
    P332 -- "taxTypes" --> P333
    P333 -- "taxTypes, registeredTaxTypes = []" --> D1
    P333 -- "TAX_TYPES_PREPARED" --> D2
```

### 3.4 Validate application

`RegValidateApplicationDelegate` → `FirstTimeRegistrationService.validateApplication`.

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D3[("D3 Taxpayer register")]:::store
    D5[("D5 Process state")]:::store

    P341("3.4.1 Load document"):::proc
    P342("3.4.2 Evaluate payload rules<br/>identityMatched, taxpayerName,<br/>identifier length ≥ 5, taxTypes ≠ ∅"):::proc
    P343("3.4.3 Check identifier<br/>not already ACTIVE"):::proc
    P344("3.4.4 Record validation<br/>message"):::proc
    P345("3.4.5 Return valid flag"):::proc

    D5 -- "drn" --> P341
    D1 -- "payload" --> P341
    P341 -- "payload fields" --> P342
    P342 -- "rule result, identifier" --> P343
    D3 -- "lifecycleState" --> P343
    P343 -- "valid" --> P344
    P344 -- "validationMessage" --> D1
    P344 -- "APPLICATION_VALIDATED" --> D2
    P344 -- "valid, message, taxTypes, taxpayerName" --> P345
    P345 -- "valid" --> D5
```

### 3.5 Register tax type (per element of `taxTypes`)

`RegRegisterTaxTypeDelegate` → `FirstTimeRegistrationService.registerTaxType`.

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D5[("D5 Process state")]:::store

    P351("3.5.1 Load document,<br/>normalise taxType"):::proc
    P352("3.5.2 Append taxType if absent"):::proc
    P353("3.5.3 Merge registeredTaxTypes"):::proc
    P354("3.5.4 Compute registration ref<br/>identifierValue-taxType"):::proc

    D5 -- "drn, taxType" --> P351
    D1 -- "registeredTaxTypes" --> P351
    P351 -- "taxType, registered list" --> P352
    P352 -- "registeredTaxTypes" --> P353
    P353 -- "registeredTaxTypes" --> D1
    P353 -- "TAX_TYPE_RECORDED" --> D2
    P353 -- "identifierValue" --> P354
    P354 -- "registeredTaxTypes, lastRegistrationRef" --> D5
```

### 5.1 Activate taxpayer and obligations

`RegFinalizeRegistrationDelegate` → `FirstTimeRegistrationService.finalizeRegistration` →
`TaxpayerRegistryService.activateTaxpayer`.

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D3[("D3 Taxpayer register")]:::store
    D4[("D4 Tax obligations")]:::store
    D5[("D5 Process state")]:::store
    D7[("D7 Audit trail")]:::store

    P511("5.1.1 Load document"):::proc
    P512("5.1.2 Idempotency check<br/>APPROVED + taxpayerId"):::proc
    P513("5.1.3 Select tax types<br/>registeredTaxTypes else taxTypes"):::proc
    P514("5.1.4 Upsert taxpayer<br/>lifecycle ACTIVE"):::proc
    P515("5.1.5 Create missing<br/>obligations"):::proc
    P516("5.1.6 Approve document"):::proc
    P517("5.1.7 Return result"):::proc

    D5 -- "drn" --> P511
    D1 -- "status, taxpayerId, payload" --> P511
    P511 -- "document" --> P512
    P512 -- "already approved: taxpayerId,<br/>registeredTaxTypes" --> P517
    P512 -- "not yet approved: payload" --> P513
    P513 -- "identifierType, identifierValue, taxpayerName,<br/>email, telephone, taxTypes" --> P514
    D3 -- "existing taxpayer by identifier" --> P514
    P514 -- "taxpayerType, identifier, displayName,<br/>email, telephone, ACTIVE, timestamps" --> D3
    D3 -. "row change" .-> D7
    P514 -- "taxpayerId, taxTypes" --> P515
    D4 -- "existing tax types for taxpayer" --> P515
    P515 -- "obligation rows (ACTIVE,<br/>registrationRef = identifier-taxType)" --> D4
    D4 -. "row change" .-> D7
    P515 -- "taxpayerId" --> P516
    P516 -- "status APPROVED, taxpayerId,<br/>payload approved = true" --> D1
    D1 -. "row change" .-> D7
    P516 -- "APPROVED event" --> D2
    P516 -- "taxpayerId, taxTypes" --> P517
    P517 -- "registrationStatus = ACTIVE, taxpayerId" --> D5
```

### 5.2 Mark application rejected

`RegRejectRegistrationDelegate` → `FirstTimeRegistrationService.rejectRegistration`.
Reached from three paths: identity not matched (3.2), application invalid (3.4), officer rejected (4.2).

```mermaid
flowchart LR
    classDef proc fill:#d6e9ff,stroke:#1f5fa8,color:#000
    classDef store fill:#e3f5e1,stroke:#2e7d32,color:#000

    D1[("D1 Registration document")]:::store
    D2[("D2 Document event log")]:::store
    D5[("D5 Process state")]:::store

    P521("5.2.1 Load document"):::proc
    P522("5.2.2 Skip if already REJECTED"):::proc
    P523("5.2.3 Reject document"):::proc
    P524("5.2.4 Return result"):::proc

    D5 -- "drn, reviewComment" --> P521
    D1 -- "status" --> P521
    P521 -- "status" --> P522
    P522 -- "already rejected" --> P524
    P522 -- "drn, reviewComment" --> P523
    P523 -- "status REJECTED, approved = false,<br/>reviewComment" --> D1
    P523 -- "REJECTED event" --> D2
    P523 -- "rejected" --> P524
    P524 -- "registrationStatus = REJECTED,<br/>validationMessage = reviewComment" --> D5
```

---

## Balancing notes

- Every Level 0 flow appears on Level 1, and every Level 1 flow into or out of a process is
  carried by at least one of its Level 2 children (e.g. the NIRA/URSB flows of 1.0 and 3.0
  are realised by 1.2 and 3.2, and detailed in 1.2.x / 3.2.x).
- 4.0 and 6.0 have no Level 3 because their Level 2 processes are already primitive
  (a single form submission and a single read-and-map).
- D7 (audit) is not written by any process directly; it is a side effect of JPA
  `@PostPersist` / `@PostUpdate` on D1, D3 and D4 rows, so it is shown with dotted flows.
- D6 is in-memory in the current stub: OTPs do not survive a registration-service restart,
  and any code of 4+ characters is accepted for an issued key.
