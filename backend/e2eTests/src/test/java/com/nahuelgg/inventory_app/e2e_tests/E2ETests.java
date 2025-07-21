package com.nahuelgg.inventory_app.e2e_tests;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.RestAssured;
import io.restassured.response.Response;

@Testcontainers
public class E2ETests {
  // Salvo el nombre del container de la base de datos, el resto debe coincidir con el nombre de los contenedores originales, que a su vez
  // coinciden con los hosts de las llamadas internas que hacen los microservicios
  private static Map<String, String> containerAliases = Map.of(
    "productAlias", "api-products",
    "userAlias", "api-users",
    "inventoryAlias", "api-inventories",
    "databaseAlias", "database-e2e"
  );

  private static enum databaseNames {
    products, inventories, users
  }

  private static String jwt_key = "TestSecretKeyForJWT1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ=";

  private static Map<String, String> setInitialEnvVariables(databaseNames databaseName) {
    return Map.of(
      "SPRING_PROFILES_ACTIVE", "e2e",
      "SPRING_DATASOURCE_URL", "jdbc:mysql://%s:3306/%s".formatted(
        containerAliases.get("databaseAlias"), databaseName.toString()),
      "SPRING_DATASOURCE_USERNAME", "root",
      "SPRING_DATASOURCE_PASSWORD", "root",
      "jwt_key", jwt_key
    );
  }

  static Network network = Network.newNetwork();
  
  @SuppressWarnings("resource")
  @Container
  static MySQLContainer<?> databaseService = new MySQLContainer<>("mysql:8.0.42")
    .withUsername("root")
    .withPassword("root")
    .withNetwork(network)
    .withNetworkAliases(containerAliases.get("databaseAlias"))
    .withInitScript("initDatabase.sql");

  @SuppressWarnings("resource")
  @Container
  static GenericContainer<?> productsService = new GenericContainer<>("api-products:latest")
    .withExposedPorts(8081)
    .withEnv(setInitialEnvVariables(databaseNames.products))
    .withNetwork(network)
    .withNetworkAliases(containerAliases.get("productAlias"))
    .dependsOn(databaseService);
    
    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> usersService = new GenericContainer<>("api-users:latest")
    .withExposedPorts(8082)
    .withEnv(setInitialEnvVariables(databaseNames.users))
    .withNetworkAliases(containerAliases.get("userAlias"))
    .withNetwork(network)
    .dependsOn(databaseService);

  @SuppressWarnings("resource")
  @Container
  static GenericContainer<?> inventoriesService = new GenericContainer<>("api-inventories:latest")
    .withExposedPorts(8083)
    .withEnv(setInitialEnvVariables(databaseNames.inventories))
    .withNetworkAliases(containerAliases.get("inventoryAlias"))
    .withNetwork(network)
    .dependsOn(databaseService);

  @SuppressWarnings("resource")
  @Container
  static GenericContainer<?> gatewayService = new GenericContainer<>("api-gateway:latest")
    .withExposedPorts(8080)
    .withEnv(Map.of(
      "jwt_key", jwt_key,
      "SPRING_PROFILES_ACTIVE", "e2e"
    ))
    .withNetwork(network)
    .withStartupTimeout(Duration.ofSeconds(60))
    .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200))
    .dependsOn(databaseService, productsService, inventoriesService, usersService);

  /* private String generateUrlWithParams(Map<String, Object> params) {
    String temp = "?";
    for (int i = 0; i < params.values().size(); i++) {
      //params.
    }
    return "";
  } */

  @Test
  void accountRegistration_saveAccountAndAdminUserInDB() {
    String baseUrl = "http://" + gatewayService.getHost() + ":" + gatewayService.getMappedPort(8080);
    String completeUrl = baseUrl + "/account/register?username=user&password=1234&passwordRepeated=1234" + 
      "&adminPassword=4321&adminPasswordRepeated=4321";
    Response response = RestAssured.given().when().post(completeUrl).andReturn();

    System.out.println(gatewayService.getLogs());
    System.out.println(usersService.getLogs());
    System.out.println(response.asPrettyString());

    assertEquals(201, response.getStatusCode());
  }
}