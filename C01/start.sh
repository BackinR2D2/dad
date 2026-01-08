#!/usr/bin/env bash
set -euo pipefail

log(){ echo "[c01] $*"; }

# SNMP (disable noisy systemstats module)
log "Starting snmpd..."
snmpd -I -systemstats -f -Lo &
sleep 1

# Wait for JMS broker (C02)
BROKER_HOST="${JMS_BROKER_HOST:-c02}"
BROKER_PORT="${JMS_BROKER_PORT:-61616}"

log "Waiting for JMS broker at ${BROKER_HOST}:${BROKER_PORT} ..."
for i in $(seq 1 60); do
  if (echo >/dev/tcp/${BROKER_HOST}/${BROKER_PORT}) >/dev/null 2>&1; then
    log "JMS broker is UP."
    break
  fi
  log "JMS broker not ready yet (${i}/60), retrying in 2s..."
  sleep 2
done

if ! (echo >/dev/tcp/${BROKER_HOST}/${BROKER_PORT}) >/dev/null 2>&1; then
  log "ERROR: JMS broker not reachable at ${BROKER_HOST}:${BROKER_PORT}"
  exit 1
fi

log "Starting C01 app..."
exec java -jar /app/app.jar