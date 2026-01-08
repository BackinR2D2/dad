#!/usr/bin/env bash
set -e
docker compose down
echo "Stopped."

# chmod +x scripts/stop-all.sh