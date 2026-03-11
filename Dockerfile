# ---------- Build Spring Boot ----------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B clean package -DskipTests


# ---------- Get Kafka Runtime ----------
FROM apache/kafka:latest AS kafka


# ---------- Final Image ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy Kafka from official image
COPY --from=kafka /opt/kafka /opt/kafka

# Copy Spring Boot jar
COPY --from=builder /build/target/blogger-hub-0.0.1-SNAPSHOT.jar app.jar

# Copy startup script
COPY start.sh /start.sh
RUN chmod +x /start.sh

# -------- Environment Variables --------
ENV CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
    KAFKA_NODE_ID=1 \
    KAFKA_PROCESS_ROLES=broker,controller \
    KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
    KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
    KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
    KAFKA_LOG_DIRS=/var/lib/kafka/data \
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
    KAFKA_AUTO_CREATE_TOPICS_ENABLE=false \
    SPRING_PROFILES_ACTIVE=staging

EXPOSE 8080

ENTRYPOINT ["/start.sh"]