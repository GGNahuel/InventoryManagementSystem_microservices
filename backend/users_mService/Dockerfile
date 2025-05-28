FROM openjdk:17-jdk-slim

WORKDIR /backend/users

COPY ./ .

RUN chmod +x mvnw

EXPOSE 8082

CMD ["./mvnw", "spring-boot:run"]
# ToDo: cambiar este comando cuando se pase a producción