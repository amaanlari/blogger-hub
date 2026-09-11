# syntax=docker/dockerfile:1

# Single-container image for Render: an embedded single-node Kafka broker (KRaft mode) plus the
# Spring Boot app (which already has the React SPA baked into its JAR). Render web services run
# exactly one container, so the broker has to live alongside the app rather than beside it.
#
# Kafka listens on loopback only and is never exposed publicly; the only port Render routes to is
# the app's $PORT.

# ---------------------------------------------------------------------------------------------
# Stage 1: build the fat JAR (Maven drives the Vite build via frontend-maven-plugin)
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Dependency resolution is its own layer so that editing source doesn't re-download the world.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline -DskipFrontend=true

# frontend-maven-plugin downloads its own pinned Node/npm into frontend/node and runs `npm ci`,
# so the build needs the frontend sources here — `src/` alone is not enough.
COPY frontend ./frontend
COPY src ./src

RUN ./mvnw -B clean package -DskipTests \
    && mv target/blogger-hub-*.jar /build/app.jar

# ---------------------------------------------------------------------------------------------
# Stage 2: runtime — JRE + Kafka distribution + the JAR
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Copied straight out of the official Apache image rather than curl'd at build time: the version
# is pinned by the image tag and there's no network dependency during the Render build.
COPY --from=apache/kafka:3.9.1 /opt/kafka /opt/kafka

ENV KAFKA_HOME=/opt/kafka \
    KAFKA_DATA_DIR=/var/lib/kafka/data \
    PATH="/opt/kafka/bin:${PATH}"

# site-docs is ~20MB of HTML the broker never reads.
RUN rm -rf /opt/kafka/site-docs \
    && useradd --create-home --shell /bin/bash spring \
    && mkdir -p "${KAFKA_DATA_DIR}" \
    && chown -R spring:spring "${KAFKA_DATA_DIR}" /opt/kafka

WORKDIR /app

COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar
COPY --chown=spring:spring start.sh ./start.sh
RUN chmod +x ./start.sh

USER spring

# Heap budgets have to be set explicitly: two JVMs share one container, and Kafka's shipped
# default is a 1GB heap that would starve the app. These suit Render's 2GB (1c-2g) plan and can
# be overridden with env vars on smaller or larger instances.
ENV KAFKA_HEAP_OPTS="-Xms256m -Xmx512m" \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=45 -XX:+ExitOnOutOfMemoryError"

# Documentation only — Render routes to whatever $PORT the process binds, and application.yaml
# already reads `server.port: ${PORT:8080}`.
EXPOSE 8080

ENTRYPOINT ["./start.sh"]
