# === Stage 1: Build the application ===
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests


# === Stage 2: Runtime image ===
FROM eclipse-temurin:21-jre AS runtime

RUN useradd -m spring
USER spring

WORKDIR /app

COPY --from=builder /app/target/blogger-hub-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","app.jar","--spring.profiles.active=${SPRING_PROFILES_ACTIVE}","--spring.config.additional-location=${SPRING_CONFIG_ADDITIONAL_LOCATION}"]