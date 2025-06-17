FROM openjdk:17-jdk-slim

WORKDIR /backend/gateway

COPY ./ .

RUN chmod +x mvnw

EXPOSE 8080

CMD ["./mvnw", "spring-boot:run"]
# ToDo: cambiar este comando cuando se pase a producción