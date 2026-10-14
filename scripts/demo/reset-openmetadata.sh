#!/usr/bin/env bash
# Demo: wipe OpenMetadata (MySQL + ES + ingestion) and start from a clean catalog.
# Does NOT touch Oracle or Spring Boot microservices.
#
# Usage:
#   ./scripts/demo/reset-openmetadata.sh          # interactive confirm
#   ./scripts/demo/reset-openmetadata.sh --yes     # no prompt
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
source "${SCRIPT_DIR}/lib.sh"

require_docker

YES=0
for arg in "$@"; do
  case "$arg" in
    -y|--yes) YES=1 ;;
    -h|--help)
      cat <<'EOF'
Wipe OpenMetadata data volumes and recreate the OM stack (fresh catalog).

  ./scripts/demo/reset-openmetadata.sh
  ./scripts/demo/reset-openmetadata.sh --yes

Preserved: Oracle (oracle-data), governance / domain app data.
Removed:   OM MySQL, Elasticsearch, Airflow/ingestion volumes
           (services, ingestions, DQ tests, glossary, users except defaults after migrate).
EOF
      exit 0
      ;;
    *)
      fail "Unknown argument: $arg (try --help)"
      exit 1
      ;;
  esac
done

info "Reset OpenMetadata for a fresh demo"
warn "This deletes all OpenMetadata catalog data (services, tables, DQ, glossary)."

if [[ "$YES" -ne 1 ]]; then
  printf 'Continue? [y/N] '
  read -r answer
  case "$answer" in
    y|Y|yes|YES) ;;
    *)
      dim "Aborted."
      exit 0
      ;;
  esac
fi

info "Stopping OpenMetadata containers..."
"${COMPOSE[@]}" stop "${OM_SERVICES[@]}" 2>/dev/null || true
"${COMPOSE[@]}" rm -f "${OM_SERVICES[@]}" 2>/dev/null || true
ok "OpenMetadata containers removed"

info "Deleting OpenMetadata volumes..."
for vol in "${OM_VOLUMES[@]}"; do
  if docker volume inspect "$vol" >/dev/null 2>&1; then
    docker volume rm "$vol" >/dev/null
    ok "removed ${vol}"
  else
    dim "skip missing volume ${vol}"
  fi
done

info "Starting fresh OpenMetadata stack..."
"${COMPOSE[@]}" up -d \
  openmetadata-mysql \
  openmetadata-elasticsearch \
  openmetadata-migrate \
  openmetadata-ingestion \
  openmetadata-server

info "Waiting for OpenMetadata API..."
wait_http "OpenMetadata" "http://localhost:8585/api/v1/system/version" 60 200

info "Waiting for ingestion (Airflow)..."
# Airflow UI may take longer; any HTTP response from host port means the process is up
INGEST_OK=0
for ((i = 1; i <= 36; i++)); do
  code="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 http://localhost:8084 2>/dev/null || true)"
  if [[ "$code" != "000" ]]; then
    ok "OpenMetadata ingestion is up (HTTP ${code})"
    INGEST_OK=1
    break
  fi
  if ((i % 6 == 0)); then
    dim "waiting for ingestion on :8084 (attempt ${i}/36)..."
  fi
  sleep 5
done
if [[ "$INGEST_OK" -ne 1 ]]; then
  warn "Ingestion UI not ready yet — check: docker logs camunda-tutorial-openmetadata-ingestion"
fi

echo
ok "OpenMetadata is ready for a fresh demo"
step "UI:       http://localhost:8585   (admin@open-metadata.org / admin)"
step "Airflow:  http://localhost:8084   (admin / admin)"
