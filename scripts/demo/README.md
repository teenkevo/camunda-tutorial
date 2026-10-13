# Demo scripts

Reusable helpers for live demos. Shared colors / compose wiring live in `lib.sh`.

| Script | Purpose |
|--------|---------|
| [`reset-openmetadata.sh`](reset-openmetadata.sh) | Wipe OpenMetadata volumes and restart a clean catalog (Oracle untouched) |

```bash
# Interactive confirm
./scripts/demo/reset-openmetadata.sh

# Non-interactive (CI / rehearsed demo)
./scripts/demo/reset-openmetadata.sh --yes
```

More demo scripts (DQ bad row, restore RFND_APPL constraint, etc.) can be added alongside these.
