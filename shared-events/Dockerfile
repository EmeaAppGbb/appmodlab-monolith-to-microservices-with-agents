# Build stage that produces the shared-events JAR
# Used as a named build stage by microservice Dockerfiles via COPY --from=shared-events
FROM eclipse-temurin:21-jdk AS shared-events
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests --no-transfer-progress && \
    mkdir -p /app/target && \
    cp target/shared-events-1.0.0.jar /app/target/shared-events-1.0.0.jar
