FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# Copy libs and services
COPY libs/ libs/
COPY services/orchestrator/ services/orchestrator/

# Build the application
RUN ./gradlew :services:orchestrator:quarkusBuild --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Copy the built Quarkus application (uber JAR)
COPY --from=build /app/services/orchestrator/build/quarkus-app/quarkus-run.jar app.jar
COPY --from=build /app/services/orchestrator/build/quarkus-app/lib/ lib/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
