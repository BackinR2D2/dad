FROM critoma/linux-u20-dev-security-ism:latest AS build
WORKDIR /src

ENV LANG=C.UTF-8 \
  LC_ALL=C.UTF-8 \
  MAVEN_OPTS="-Dfile.encoding=UTF-8"

RUN apt-get update && \
  apt-get install -y --no-install-recommends maven openjdk-17-jdk ca-certificates curl && \
  rm -rf /var/lib/apt/lists/*

# Copy pom first for layer caching
COPY app/pom.xml app/pom.xml

# Pre-fetch dependencies with retries + better network tolerance
# Use BuildKit cache for ~/.m2 to avoid re-downloading each build
RUN --mount=type=cache,target=/root/.m2 \
  bash -lc 'set -e; \
  for i in 1 2 3 4 5; do \
  echo "Maven go-offline attempt $i/5"; \
  mvn -f app/pom.xml -DskipTests -e -B \
  -Dmaven.wagon.http.retryHandler.count=5 \
  -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
  -Dmaven.wagon.http.timeout=120000 \
  -Dmaven.wagon.http.readTimeout=120000 \
  dependency:go-offline && break; \
  echo "Retrying in 3s..."; \
  sleep 3; \
  done'

# Copy sources after deps are cached
COPY app/src app/src

RUN --mount=type=cache,target=/root/.m2 \
  bash -lc 'set -e; \
  for i in 1 2 3 4 5; do \
  echo "Maven package attempt $i/5"; \
  mvn -f app/pom.xml -DskipTests -B \
  -Dmaven.wagon.http.retryHandler.count=5 \
  -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
  -Dmaven.wagon.http.timeout=120000 \
  -Dmaven.wagon.http.readTimeout=120000 \
  package && break; \
  echo "Retrying in 3s..."; \
  sleep 3; \
  done'

FROM critoma/linux-u20-dev-security-ism:latest

ARG TOMEE_VERSION=10.1.3
ENV TOMEE_HOME=/opt/tomee
ENV LANG=C.UTF-8 \
  LC_ALL=C.UTF-8

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  openjdk-17-jre-headless curl unzip snmpd ca-certificates && \
  rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt && \
  curl -fsSL -o /opt/tomee.zip \
  https://archive.apache.org/dist/tomee/tomee-${TOMEE_VERSION}/apache-tomee-${TOMEE_VERSION}-plume.zip && \
  unzip -q /opt/tomee.zip -d /opt && \
  rm -f /opt/tomee.zip && \
  ln -s /opt/apache-tomee-plume-${TOMEE_VERSION} ${TOMEE_HOME} && \
  chmod -R a+rx /opt/apache-tomee-plume-${TOMEE_VERSION}/bin

RUN rm -rf ${TOMEE_HOME}/webapps/docs \
  ${TOMEE_HOME}/webapps/manager \
  ${TOMEE_HOME}/webapps/host-manager \
  ${TOMEE_HOME}/webapps/ROOT

COPY snmpd.conf /etc/snmp/snmpd.conf
COPY tomee/tomee.xml ${TOMEE_HOME}/conf/tomee.xml
COPY start.sh /start.sh
RUN sed -i 's/\r$//' /start.sh && chmod 755 /start.sh

COPY --from=build /src/app/target/c03.war ${TOMEE_HOME}/webapps/c03.war

EXPOSE 8080
EXPOSE 161/udp
CMD ["/bin/bash", "/start.sh"]
