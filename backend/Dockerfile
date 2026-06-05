# ContractOps AI — Production Backend Dockerfile
# Multi-stage build for smaller final image

# Use the official Gradle image so we don't depend on the committed
# gradle-wrapper.jar (which is fragile through Git autocrlf / CI checkout
# and was causing "Could not find or load main class GradleWrapperMain").
FROM gradle:8.14.4-jdk21 AS build

USER root
WORKDIR /app

# build.gradle.kts redirects the build dir (to dodge OneDrive locks on Windows).
# Pin it to /app/build inside the container so the runtime stage can find the jar.
ENV CONTRACTOPS_BUILD_DIR=/app/build

# Copy build files first (better layer caching)
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source
COPY src ./src

# Build the application
RUN gradle clean bootJar --no-daemon -x test

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
