FROM critoma/linux-u20-dev-security-ism:latest AS build
WORKDIR /src

ENV LANG=C.UTF-8 \
  LC_ALL=C.UTF-8 \
  MAVEN_OPTS="-Dfile.encoding=UTF-8"

RUN apt-get update && \
  apt-get install -y --no-install-recommends maven openjdk-17-jdk && \
  rm -rf /var/lib/apt/lists/*

COPY app/pom.xml app/pom.xml
COPY app/src app/src
RUN mvn -f app/pom.xml -DskipTests package

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