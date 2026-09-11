# syntax=docker/dockerfile:1

# Single-container image for Render, built for the `staging` profile: MongoDB (Atlas), Kafka
# (Aiven) and Redis are all managed services reached over TLS, so the container runs exactly one
# process — the Spring Boot app, which already has the React SPA compiled into its JAR.
#
# Nothing secret is baked in. Credentials arrive at runtime either as Render environment
# variables or as Render Secret Files under /etc/secrets (see start.sh and render.yaml).

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
# Stage 2: runtime — JRE + the JAR
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN useradd --create-home --shell /bin/bash spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar
COPY --chown=spring:spring start.sh ./start.sh
RUN chmod +x ./start.sh

USER spring

ENV SPRING_PROFILES_ACTIVE=staging \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# Documentation only — Render routes to whatever $PORT the process binds, and application.yaml
# already reads `server.port: ${PORT:8080}`.
EXPOSE 8080

ENTRYPOINT ["./start.sh"]
