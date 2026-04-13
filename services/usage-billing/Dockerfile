FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

COPY libs/ libs/
COPY services/usage-billing/ services/usage-billing/

RUN ./gradlew :services:usage-billing:build --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /app/services/usage-billing/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
