# syntax=docker/dockerfile:1

# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and dependency definition files first for caching
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Ensure gradlew has executable permissions and download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code and build production jar
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime environment
FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for container health check
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as dedicated unprivileged user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact
COPY --from=builder --chown=spring:spring /app/build/libs/amortizacion-0.0.1-SNAPSHOT.jar app.jar

# Application environment configuration
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/loans || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

