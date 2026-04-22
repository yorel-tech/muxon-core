FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends nodejs npm \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

COPY libs/ libs/
COPY providers/ providers/
COPY integration-tests/ integration-tests/
COPY openapi/ openapi/
COPY services/ services/

RUN ./gradlew :services:console-proxy:bootJar --no-daemon -x test \
    && JAR="$(ls services/console-proxy/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1)" \
    && test -n "$JAR" \
    && cp "$JAR" /app/app.jar

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
