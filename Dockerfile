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

ENV SPRING_PROFILES_ACTIVE=staging \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# 1. Point the variable to the safe /app path where 'spring' user can write files
ENV KAFKA_SSL_TRUSTSTORE_PATH=/app/ca.pem

EXPOSE 8080

# 2. Extract the Vercel secret variable to the file right before booting Java
ENTRYPOINT ["sh", "-c", "echo \"$KAFKA_CA_CERT\" > /app/ca.pem && exec java $JAVA_OPTS -jar /app/app.jar"]
