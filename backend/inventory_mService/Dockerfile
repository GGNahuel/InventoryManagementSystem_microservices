FROM openjdk:17-jdk-slim

WORKDIR /backend/inventories

COPY ./ .

RUN chmod +x mvnw

EXPOSE 8083

CMD ["./mvnw", "spring-boot:run"]