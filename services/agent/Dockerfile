FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# Copy libs and services
COPY libs/ libs/
COPY services/agent/ services/agent/

# Build the application
RUN ./gradlew :services:agent:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/services/agent/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
