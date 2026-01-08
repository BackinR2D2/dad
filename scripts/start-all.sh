#!/usr/bin/env bash
set -e

echo "Starting full stack (backend + frontend) ..."
docker compose down
docker compose up -d --build

echo ""
echo "All containers are up."
echo "Open: http://localhost:5173"
echo ""
docker compose ps

# chmod +x scripts/start-all.sh