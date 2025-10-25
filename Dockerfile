FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package


FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get upgrade -y --no-install-recommends \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*


VOLUME /tmp
RUN apt-get update && \
    apt-get upgrade -y --no-install-recommends && \
    apt-get install -y --no-install-recommends openssl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
COPY --from=build /home/app/target/flowbot-service-0.0.1-SNAPSHOT.jar app.jar
ADD https://storage.googleapis.com/datadog_apm/dd-java-agent.jar /dd-java-agent.jar
ADD https://storage.googleapis.com/datadog_apm/YC.tgz /YC.tgz
ADD https://storage.googleapis.com/datadog_apm/jdk-17_linux-x64_bin.tar.gz /jdk-17_linux-x64_bin.tar.gz

ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
