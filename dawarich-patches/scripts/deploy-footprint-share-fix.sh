#!/usr/bin/env bash
# Deploy Dawarich shared-trip-map fix on VPS (footprint.mantoublog.top)
# Usage (on VPS as root):
#   bash deploy-footprint-share-fix.sh /path/to/dawarich

set -euo pipefail

DAWARICH_DIR="${1:-/opt/dawarich}"
PATCH_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${DAWARICH_DIR}/app/javascript/controllers/shared_trip_map_controller.js"

if [[ ! -d "$DAWARICH_DIR" ]]; then
  echo "Dawarich directory not found: $DAWARICH_DIR" >&2
  exit 1
fi

cp "$PATCH_DIR/app/javascript/controllers/shared_trip_map_controller.js" "$TARGET"
echo "Patched: $TARGET"

cd "$DAWARICH_DIR"

if docker compose ps --services 2>/dev/null | grep -q .; then
  echo "Rebuilding web assets inside Docker..."
  docker compose exec -T web bin/rails assets:precompile
  docker compose restart web
elif command -v docker >/dev/null && docker ps --format '{{.Names}}' | grep -qi dawarich; then
  CONTAINER="$(docker ps --format '{{.Names}}' | grep -i dawarich | head -1)"
  echo "Rebuilding in container: $CONTAINER"
  docker exec "$CONTAINER" bin/rails assets:precompile
  docker restart "$CONTAINER"
else
  echo "Run assets:precompile and restart your Dawarich web process manually."
fi

echo "Done. Hard-refresh the share page (Ctrl+F5)."
