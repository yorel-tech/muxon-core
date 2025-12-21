FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY services/core-services/build/libs/*.jar app.jar
COPY services/bootstrap-initializer/build/libs/*.jar bootstrap.jar

# Copy entrypoint script
COPY scripts/core-services-entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
