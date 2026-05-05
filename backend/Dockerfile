# Build stage: compile the Spring Boot JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies before copying sources
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage: lightweight JRE image, runs as non-root
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user to limit container privileges
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]