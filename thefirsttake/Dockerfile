# Build stage
FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY thefirsttake/ .
WORKDIR /app
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# Runtime stage  
FROM openjdk:21-jdk-slim
WORKDIR /app

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/thefirsttake-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8000/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]
