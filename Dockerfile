# === Stage 1: Build the application ===
FROM eclipse-temurin:21-jdk AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper and dependency files for caching dependencies
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# === Stage 2: Create a minimal runtime image ===
FROM eclipse-temurin:21-jre AS runtime

## Set non-root user for security
#RUN useradd -m spring
#USER spring

# Set working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/blogger-hub-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080


# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
