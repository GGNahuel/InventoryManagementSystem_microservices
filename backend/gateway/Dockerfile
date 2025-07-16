FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /backend/gateway

COPY ./ .

# Importantt: saltea los tests porque se deberían hacer en el CI/CD
RUN mvn clean package -DskipTests 


FROM openjdk:17-jdk-slim

WORKDIR /backend/gateway

COPY --from=builder /backend/gateway/target/*.jar /api/gateway.jar

EXPOSE 8080

CMD ["java", "-jar", "/api/gateway.jar"]