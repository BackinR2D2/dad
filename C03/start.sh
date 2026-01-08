#!/usr/bin/env bash
set -euo pipefail

log(){ echo "[c03] $*"; }

log "Starting snmpd..."
snmpd -I -systemstats -I -systemstats_linux -f -Lo &
sleep 1

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

export JAVA_OPTS="${JAVA_OPTS:-} -Djava.net.preferIPv4Stack=true"

log "Starting TomEE..."
exec /opt/tomee/bin/catalina.sh run