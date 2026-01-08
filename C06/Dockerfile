FROM critoma/linux-u20-dev-security-ism:latest

WORKDIR /app

# Add MongoDB 4.4 repository (last version supporting Ubuntu 20.04)
RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  curl ca-certificates gnupg \
  mysql-server mysql-client \
  snmp snmpd procps \
  && rm -rf /var/lib/apt/lists/*

# Install MongoDB 4.4
RUN curl -fsSL https://www.mongodb.org/static/pgp/server-4.4.asc | apt-key add - && \
  echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-4.4.list && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends mongodb-org && \
  rm -rf /var/lib/apt/lists/*

# Node.js 20 (Ubuntu 20.04)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends nodejs && \
  rm -rf /var/lib/apt/lists/*

COPY package.json package-lock.json* ./
RUN npm ci --omit=dev

COPY . .
COPY snmpd.conf /etc/snmp/snmpd.conf
COPY start.sh /start.sh
RUN sed -i 's/\r$//' /start.sh && chmod +x /start.sh

EXPOSE 3000
EXPOSE 27017
EXPOSE 3306
EXPOSE 161/udp

CMD ["/start.sh"]