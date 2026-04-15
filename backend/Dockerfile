# Etapa 1: Construcción (Build)
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiamos primero el pom.xml y descargamos dependencias (optimiza el caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run) - Usamos una imagen más ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el JAR generado desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]