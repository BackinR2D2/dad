#!/usr/bin/env bash
set -euo pipefail

log(){ echo "[C06] $*"; }

log "Cleaning up old processes..."
pkill -9 mongod || true
pkill -9 mysqld || true
pkill -9 snmpd || true
sleep 2

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DB:-${MYSQL_DATABASE:-dad}}"
MYSQL_USER="${MYSQL_USER:-dad}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-dad}"

MYSQL_SOCK="/var/run/mysqld/mysqld.sock"
MYSQL_PID="/var/run/mysqld/mysqld.pid"
MYSQL_DATADIR="/var/lib/mysql"

# ---- SNMP
log "Starting snmpd..."
snmpd -f -Lo -I -systemstats &
sleep 1

# ---- MongoDB
log "Starting MongoDB (mongod)..."
mkdir -p /data/db /var/log
chown -R mongodb:mongodb /data/db 2>/dev/null || true
mongod --bind_ip 0.0.0.0 --dbpath /data/db --logpath /var/log/mongod.log --logappend &
sleep 3

# ---- MySQL
log "Preparing MySQL directories/permissions..."
mkdir -p /var/run/mysqld "${MYSQL_DATADIR}"
chown -R mysql:mysql /var/run/mysqld "${MYSQL_DATADIR}"
chmod 755 /var/run/mysqld
rm -f "${MYSQL_SOCK}" "${MYSQL_PID}"

if [ ! -d "${MYSQL_DATADIR}/mysql" ]; then
  log "Initializing MySQL data directory..."
  mysqld --initialize-insecure --user=mysql --datadir="${MYSQL_DATADIR}"
fi

log "Creating MySQL init file..."
cat > /tmp/mysql-init.sql <<SQLEOF
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`;
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'localhost';
FLUSH PRIVILEGES;
SQLEOF

log "Starting MySQL with init-file..."
mysqld \
  --user=mysql \
  --bind-address=0.0.0.0 \
  --datadir="${MYSQL_DATADIR}" \
  --socket="${MYSQL_SOCK}" \
  --pid-file="${MYSQL_PID}" \
  --log-error=/tmp/mysql-error.log \
  --log-error-verbosity=3 \
  --default-authentication-plugin=mysql_native_password \
  --init-file=/tmp/mysql-init.sql \
  &

log "Waiting for MySQL..."
for i in $(seq 1 30); do
  if mysqladmin --socket="${MYSQL_SOCK}" ping --silent >/dev/null 2>&1; then
    log "MySQL is ready!"
    break
  fi
  sleep 1
done

if ! mysqladmin --socket="${MYSQL_SOCK}" ping --silent >/dev/null 2>&1; then
  log "ERROR: MySQL failed to start. Checking logs..."
  tail -n 100 /tmp/mysql-error.log || true
  exit 1
fi

log "MySQL configuration complete!"

log "Starting Node.js API..."
export PORT="${PORT:-3000}"
export MONGO_URL="${MONGO_URL:-mongodb://127.0.0.1:27017}"
export MONGO_DB="${MONGO_DB:-dad_metrics}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_DB="${MYSQL_DB:-dad}"
export MYSQL_USER="${MYSQL_USER:-dad}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-dad}"
export SNMP_COMMUNITY="${SNMP_COMMUNITY:-public}"

exec node app.js