# syntax=docker/dockerfile:1

# Blogger Hub — everything in one Dockerfile.
#
# Build:  Maven compiles the Java app and, via frontend-maven-plugin, runs the React/Vite build
#         whose output lands in src/main/resources/static/ — so the SPA is packaged inside the
#         same executable JAR.
# Run:    one process, `java -jar`. MongoDB (Atlas), Kafka (Aiven) and Redis are managed services
#         reached over TLS, so there is nothing else to start.
#
# No secrets are baked in; see .dockerignore. Credentials arrive at runtime as environment
# variables, or as a Render Secret File (see SPRING_CONFIG_ADDITIONAL_LOCATION below).

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
COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar
USER spring

ENV SPRING_PROFILES_ACTIVE=staging \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# Render mounts Secret Files at /etc/secrets/<filename>. Spring reads this variable itself, so all
# credentials can be supplied as one uploaded YAML instead of ~15 dashboard fields. `optional:`
# means the app still starts fine when the file isn't there and everything comes from env vars.
# Plain environment variables override the file either way.
ENV SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/etc/secrets/application-staging.yaml

# The Aiven CA certificate the Kafka client validates the broker against. A public certificate,
# not a credential, but it is uploaded as a Secret File alongside the config above.
ENV KAFKA_SSL_TRUSTSTORE_PATH=/etc/secrets/ca.pem

# Documentation only — Render routes to whatever $PORT the process binds, and application.yaml
# already reads `server.port: ${PORT:8080}`.
EXPOSE 8080

# `exec` so the JVM is PID 1 and receives SIGTERM directly, letting Spring Boot shut down
# gracefully. `sh -c` is what word-splits $JAVA_OPTS into separate JVM flags.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
