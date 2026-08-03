# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN for i in 1 2 3; do mvn dependency:go-offline -B && break || (echo "Attempt $i failed, retrying..." && sleep 10); done
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/db/migration ./db/migration

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
