FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /backend/inventories

COPY ./ .

RUN mvn clean package -DskipTests


FROM openjdk:17-jdk-slim

WORKDIR /backend/inventories

COPY --from=builder /backend/inventories/target/*.jar /api/inventories_service.jar

EXPOSE 8083

CMD ["java", "-jar", "/api/inventories_service.jar"]