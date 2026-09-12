# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------------------------
# Stage 1 — build the fat JAR (Java + React)
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Dependency resolution is its own layer so editing source doesn't re-download the world.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline -DskipFrontend=true

# frontend-maven-plugin downloads its own pinned Node/npm and runs `npm ci`, so the build needs
# the frontend sources here — `src/` alone is not enough.
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

# 1. Create a local certs folder inside the app directory
RUN mkdir -p /app/certs

# 2. Copy the ca.pem file from your repository and set ownership to 'spring'
COPY --chown=spring:spring ca.pem /app/certs/ca.pem
COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# The Aiven CA certificate the Kafka client validates the broker against. A public certificate,
# not a credential, but it is uploaded as a Secret File alongside the config above.
ENV KAFKA_SSL_TRUSTSTORE_PATH=/app/ca.pem

# Documentation only — Render routes to whatever $PORT the process binds, and application.yaml
# already reads `server.port: ${PORT:8080}`.
EXPOSE 8080

# 2. Extract the Vercel secret variable to the file right before booting Java
ENTRYPOINT ["sh", "-c", "echo \"$KAFKA_CA_CERT\" > /app/ca.pem && exec java $JAVA_OPTS -jar /app/app.jar"]
