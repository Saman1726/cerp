# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy only pom first to leverage Docker layer caching
COPY pom.xml .
# Then copy sources
COPY src ./src
# Build the application (skip tests for faster image builds)
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

# Copy boot jar
COPY --from=build /app/target/vendor-service-java-0.1.0-SNAPSHOT.jar /app/app.jar

# App port
EXPOSE 8085

# Runtime JVM options can be supplied via JAVA_OPTS env
ENV JAVA_OPTS=""

# Run the app
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]