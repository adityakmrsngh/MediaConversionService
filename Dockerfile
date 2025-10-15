# Multi-stage build for MediaConversionService
# Optimized for Google Cloud Run deployment

# Stage 1: Build stage
FROM openjdk:17-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Install Tesseract OCR and language packs
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Verify Tesseract installation
RUN tesseract --list-langs && tesseract -v

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create temp directory for conversion processing
RUN mkdir -p /tmp/conversion && chown -R appuser:appuser /tmp/conversion

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership of the app directory
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (Cloud Run will set PORT environment variable)
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables for Cloud Run optimization
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloudrun

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

# Labels for better container management
LABEL maintainer="Zendly MediaConversionService Team"
LABEL version="1.0.0"
LABEL description="Media Conversion Service for document, audio, and image text extraction"
LABEL cloud.platform="Google Cloud Run"
