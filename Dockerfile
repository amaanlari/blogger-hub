# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------------------------
# Stage 1 — build the fat JAR (Java + React) - (Unchanged)
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline -DskipFrontend=true

COPY frontend ./frontend
COPY src ./src

RUN ./mvnw -B clean package -DskipTests \
    && mv target/blogger-hub-*.jar /build/app.jar

# ---------------------------------------------------------------------------------------------
# Stage 2 — runtime
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN useradd --create-home --shell /bin/bash spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENV KAFKA_SSL_TRUSTSTORE_PATH=/tmp/ca.pem

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "set -eu; printf '%s\\n' \"$KAFKA_CA_CERT\" > /tmp/ca.pem; echo \"CA size: $(wc -c < /tmp/ca.pem) bytes\"; echo \"CA header: $(head -n 1 /tmp/ca.pem)\"; echo \"CA footer: $(tail -n 1 /tmp/ca.pem)\"; exec java $JAVA_OPTS -jar /app/app.jar"]