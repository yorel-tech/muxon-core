FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app
COPY services/muxon-initializer/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
