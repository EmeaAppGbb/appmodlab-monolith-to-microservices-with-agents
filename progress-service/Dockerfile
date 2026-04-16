# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Copy shared-events library for local dependency resolution
COPY --from=shared-events /app/target/shared-events-1.0.0.jar /root/.m2/repository/com/eduverse/shared-events/1.0.0/shared-events-1.0.0.jar
RUN apt-get update && apt-get install -y maven && \
    mvn package -DskipTests --no-transfer-progress

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
