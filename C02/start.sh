#!/usr/bin/env bash
set -euo pipefail

log(){ echo "[c02] $*"; }

log "Starting snmpd..."
snmpd -I -systemstats -f -Lo &
sleep 1

log "Starting ActiveMQ Classic..."
exec /opt/activemq/bin/activemq console