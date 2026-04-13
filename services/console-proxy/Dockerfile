FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

COPY libs/ libs/
COPY services/console-proxy/ services/console-proxy/

RUN ./gradlew :services:console-proxy:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /app/services/console-proxy/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
