# Demo scripts

Reusable helpers for live demos. Shared colors / compose wiring live in `lib.sh`.

| Script | Purpose |
|--------|---------|
| [`reset-openmetadata.sh`](reset-openmetadata.sh) | Wipe OpenMetadata volumes and restart a clean catalog (Oracle untouched) |
| [`inject-refund-missing-application-id.sh`](inject-refund-missing-application-id.sh) | Plant a null `DRN` row on `DOCUMENT_RECORD` for DQ demos; prints inspection SQL |

```bash
# Interactive confirm
./scripts/demo/reset-openmetadata.sh
./scripts/demo/inject-refund-missing-application-id.sh

# Non-interactive (CI / rehearsed demo)
./scripts/demo/reset-openmetadata.sh --yes
./scripts/demo/inject-refund-missing-application-id.sh --yes
```
