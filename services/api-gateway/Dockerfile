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
RUN ./gradlew :services:api-gateway:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/services/api-gateway/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
