FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /backend/users

COPY ./ .

RUN mvn clean package -DskipTests


FROM openjdk:17-jdk-slim

WORKDIR /backend/users

COPY --from=builder /backend/users/target/*.jar /api/users_service.jar

EXPOSE 8082

CMD ["java", "-jar", "/api/users_service.jar"]