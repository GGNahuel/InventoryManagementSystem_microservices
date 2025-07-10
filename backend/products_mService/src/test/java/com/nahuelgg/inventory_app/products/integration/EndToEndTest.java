package com.nahuelgg.inventory_app.products.integration;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EndToEndTest {
  @Autowired ObjectMapper objectMapper;
  @Autowired ProductRepository productRepository;

  @LocalServerPort
  int port;

  @Value("${JWT_KEY}")
  String secretKeyTest;

  UUID acc1Id = UUID.fromString("acc00000-0000-0000-0000-000000000001");
  UUID acc2Id = UUID.fromString("acc00000-0000-0000-0000-000000000002");
  UUID pr1, pr2, pr3, pr4, pr5;
  String baseUrl;

  private String generateToken(JwtClaimsDTO info, String username, String accId) {
    Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyTest));

    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("accountId", accId);
    
    if (info != null) {
      String userPerms = null;
      try {
        userPerms = info.getUserPerms() != null ? objectMapper.writeValueAsString(info.getUserPerms()) : "[]";
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e.getMessage());
      }

      extraClaims.put("userName", info.getUserName());
      extraClaims.put("userRole", info.getUserRole());
      extraClaims.put("isAdmin", info.isAdmin());
      extraClaims.put("userPerms", userPerms);
    } else {
      extraClaims.put("userName", null);
      extraClaims.put("userRole", null);
      extraClaims.put("isAdmin", false);
      extraClaims.put("userPerms", null);
    }

    return Jwts.builder()
      .setClaims(extraClaims)
      .setSubject(username)
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
      .signWith(key, SignatureAlgorithm.HS256)
    .compact();
  }

  @BeforeEach
  void setUpIds() {
    List<UUID> ids = productRepository.findAll().stream().map(product -> product.getId()).toList();

    if (ids.size() != 5) throw new RuntimeException("Error al guardar datos iniciales en la base de datos");

    pr1 = ids.get(0);
    pr2 = ids.get(1);
    pr3 = ids.get(2);
    pr4 = ids.get(3);
    pr5 = ids.get(4);
  }

  @Test
  void getByIds_returnsExpectedResponse() throws JsonMappingException, JsonProcessingException {
    baseUrl = "http://localhost:" + port;
    String token = generateToken(null, "username", acc1Id.toString());

    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl + "/product/ids")
      .queryParam("list", List.of(pr1.toString(), pr2.toString(), pr3.toString()))
    .toUriString();

    RestAssured.port = port;

    Response response = RestAssured.given()
      .contentType("application/json")
      .header("Authorization", "Bearer " + token)
    .when().get(completeUrl).andReturn();

    assertEquals(200, response.getStatusCode());

    ResponseDTO<List<ProductDTO>> mappedResponse = objectMapper.readValue(
      response.getBody().asString(), new TypeReference<ResponseDTO<List<ProductDTO>>>() {}
    );
    List<ProductDTO> actual = mappedResponse.getData();

    assertNull(mappedResponse.getError());
    assertEquals(3, actual.size());
    assertAll(
      () -> assertTrue(actual.stream().anyMatch(product -> product.getId().equals(pr1.toString()))),
      () -> assertTrue(actual.stream().anyMatch(product -> product.getId().equals(pr2.toString()))),
      () -> assertTrue(actual.stream().anyMatch(product -> product.getId().equals(pr3.toString())))
    );
  }
}
