package com.nahuelgg.inventory_app.users.integration;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EndToEndTest {
  private static Network network = Network.newNetwork();

  @Value("${jwt_key}")
  private static String secretKey;

  @SuppressWarnings("resource")
  static GenericContainer<?> productService = new GenericContainer<>("api-products")
    .withExposedPorts(8081)
    .withNetwork(network)
    .withNetworkAliases("api-products")
    .withEnv(Map.of(
      "SPRING_PROFILES_ACTIVE", "test",
      "jwt_key", secretKey
    ))
  ;

  @SuppressWarnings("resource")
  static GenericContainer<?> inventoryService = new GenericContainer<>("api-inventories")
    .withExposedPorts(8081)
    .withNetwork(network)
    .withNetworkAliases("api-inventories")
    .withEnv(Map.of(
      "SPRING_PROFILES_ACTIVE", "test",
      "jwt_key", secretKey
    ))
  ;

  @BeforeAll
  static void initContainers() {
    productService.start();
  }

  @AfterAll
  static void stopContainers() {
    productService.stop();
  }
}
