FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends nodejs npm \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# Copy project sources needed for orchestrator build
COPY libs/ libs/
COPY providers/ providers/
COPY integration-tests/ integration-tests/
COPY openapi/ openapi/
COPY services/ services/

# Build the runnable Spring Boot jar
# (we copy it to a stable path to avoid dealing with versioned filenames)
RUN ./gradlew :services:orchestrator:bootJar --no-daemon -x test \
    && JAR="$(ls services/orchestrator/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1)" \
    && test -n "$JAR" \
    && cp "$JAR" /app/app.jar

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
