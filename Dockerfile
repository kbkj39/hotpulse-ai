# Multi-stage build for a Maven + Spring Boot application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven configuration and source
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for faster image builds; run tests in CI)
# Use a cache for Maven local repository to speed up repeated builds (requires BuildKit)
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
