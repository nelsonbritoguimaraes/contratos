# ContractOps AI — Production Backend Dockerfile
# Multi-stage build for smaller final image

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Ensure gradlew is executable (Git on Windows may lose +x permission)
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source
COPY src ./src

# Build the application
RUN ./gradlew clean bootJar --no-daemon -x test

# ==================== Runtime Stage ====================
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S contractops && adduser -S contractops -G contractops

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R contractops:contractops /app

# Switch to non-root user
USER contractops

# Production JVM options (tune as needed)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Healthcheck (requires actuator)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
