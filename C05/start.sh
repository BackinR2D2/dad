#!/usr/bin/env bash
set -euo pipefail

: "${RMI_REGISTRY_PORT:=1099}"
: "${RMI_SERVICE_PORT:=2001}"
: "${RMI_NAME:=ZoomService}"
: "${RMI_HOSTNAME:=c05}"

if command -v snmpd >/dev/null 2>&1; then
  /usr/sbin/snmpd -f -Lo -C -c /etc/snmp/snmpd.conf &
fi

exec java -Djava.net.preferIPv4Stack=true -cp /app/classes ServerMain
