FROM critoma/linux-u20-dev-security-ism:latest

WORKDIR /opt

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

ENV ACTIVEMQ_VERSION=5.18.3
ENV ACTIVEMQ_HOME=/opt/activemq

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    snmpd \
    curl \
    ca-certificates \
    wget \
    tar \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt \
    && wget -q "https://archive.apache.org/dist/activemq/${ACTIVEMQ_VERSION}/apache-activemq-${ACTIVEMQ_VERSION}-bin.tar.gz" -O /opt/activemq.tgz \
    && tar -xzf /opt/activemq.tgz -C /opt \
    && mv "/opt/apache-activemq-${ACTIVEMQ_VERSION}" "${ACTIVEMQ_HOME}" \
    && rm -f /opt/activemq.tgz \
    && mkdir -p /etc/snmp

COPY activemq.xml ${ACTIVEMQ_HOME}/conf/activemq.xml
COPY snmpd.conf /etc/snmp/snmpd.conf
COPY start.sh /start.sh
RUN chmod +x /start.sh

EXPOSE 61616 8161
CMD ["/start.sh"]