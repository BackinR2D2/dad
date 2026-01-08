#!/bin/bash
set -euo pipefail

echo "[C04] Starting snmpd..."
snmpd -f -Lo -I -systemstats &
sleep 1

: "${RMI_PORT:=1099}"
: "${RMI_OBJ_PORT:=1109}"
: "${RMI_NAME:=ZoomService}"
: "${RMI_HOSTNAME:=c04}"

echo "[C04] Starting RMI server..."
exec java -Djava.net.preferIPv4Stack=true -cp /app/classes ServerMain
