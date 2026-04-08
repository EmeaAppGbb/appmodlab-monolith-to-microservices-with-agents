# =============================================================================
# EduVerse Academy - Multi-stage Docker Build
# Stage 1: Build the WAR artifact with Maven
# Stage 2: Deploy to Tomcat 9 on JRE 11
# =============================================================================

# --------------- Stage 1: Maven Build ---------------
FROM maven:3.8-openjdk-11 AS build

WORKDIR /app

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the WAR (skip tests for faster image builds)
COPY src ./src
RUN mvn clean package -DskipTests -B

# --------------- Stage 2: Tomcat Runtime ---------------
FROM tomcat:9-jre11-temurin

# Remove default Tomcat webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the WAR from the build stage
COPY --from=build /app/target/eduverse-academy.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
