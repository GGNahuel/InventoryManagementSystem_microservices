FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /backend/products

COPY ./ .

# Importantt: saltea los tests porque se deberían hacer en el CI/CD
RUN mvn clean package -DskipTests 


FROM openjdk:17-jdk-slim

WORKDIR /backend/products

COPY --from=builder /backend/products/target/*.jar /api/products_service.jar

EXPOSE 8081

CMD ["java", "-jar", "/api/products_service.jar"]