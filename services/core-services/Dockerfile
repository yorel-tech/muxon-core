FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# Copy libs and services
COPY openapi/ openapi/
COPY libs/ libs/
COPY providers/ providers/
COPY services/ services/

# Build the application
RUN ./gradlew :services:core-services:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/services/core-services/build/libs/*.jar app.jar

# Copy initial-config.yaml if present (for dev builds)
COPY services/core-services/src/main/resources/initial-config.yaml /etc/infron/initial-config.yaml

EXPOSE 8081

volumes:
- ./initial-config.yaml:/etc/infron/initial-config.yaml:ro
environment:
INITIAL_CONFIG_PATH: /etc/infron/initial-config.yaml

ENTRYPOINT ["java", "-jar", "app.jar"]
