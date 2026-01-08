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
WORKDIR /app

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-17-jre-headless snmpd && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /opt/java/openjdk/bin && \
    ln -sf /usr/bin/java /opt/java/openjdk/bin/java

COPY --from=build /src/app/target/app.jar /app/app.jar
COPY start.sh /start.sh
COPY snmpd.conf /etc/snmp/snmpd.conf
RUN chmod +x /start.sh

EXPOSE 8081
CMD ["/start.sh"]