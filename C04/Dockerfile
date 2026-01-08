FROM critoma/linux-u20-dev-security-ism:latest

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    openjdk-17-jdk snmpd snmp procps ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY snmpd.conf /etc/snmp/snmpd.conf
COPY rmi/ /app/rmi/
COPY start.sh /start.sh
RUN sed -i 's/\r$//' /start.sh && chmod +x /start.sh

RUN rm -rf /app/classes && mkdir -p /app/classes && \
    javac -d /app/classes $(find /app/rmi -name "*.java")

EXPOSE 161/udp
EXPOSE 1099
EXPOSE 1109
CMD ["/start.sh"]