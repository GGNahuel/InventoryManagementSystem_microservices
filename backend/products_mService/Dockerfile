FROM openjdk:17-jdk-slim

WORKDIR /backend/products

COPY ./ .

RUN chmod +x mvnw

EXPOSE 8081

CMD ["./mvnw", "spring-boot:run"]
# ToDo: cambiar este comando cuando se pase a producción