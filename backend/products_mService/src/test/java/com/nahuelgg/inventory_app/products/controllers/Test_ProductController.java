package com.nahuelgg.inventory_app.products.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.services.JwtService;
import com.nahuelgg.inventory_app.products.services.ProductService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test_ProductController {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;
  
  @MockitoBean ProductService service;
  @MockitoBean JwtService jwtService;

  UUID acc1ID = UUID.randomUUID();
  ProductEntity pr1;
  ProductDTO prDTO1;

  String accUsername = "accUsername";
  String token = "testToken";

  @BeforeEach
  void beforeEach() {
    pr1 = ProductEntity.builder()
      .id(UUID.randomUUID())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID)
    .build();

    prDTO1 = ProductDTO.builder()
      .id(pr1.getId().toString())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID.toString())
    .build();
  }

  private void configJwtMock(String userName, String userRole, boolean isAdmin, List<PermissionsForInventoryDTO> userPerms) {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject(accUsername);
      claims.put("accountId", acc1ID.toString());
      claims.put("userName", userName);
      claims.put("userRole", userRole);
      claims.put("isAdmin", isAdmin);
      claims.put("userPerms", userPerms);
      return claimGetter.apply(claims);
    });
    when(jwtService.isTokenExpired(token)).thenReturn(false);
  }

  private HttpEntity<String> generateRequestWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return new HttpEntity<String>(headers);
  }

  @Test
  void getByIds() {
    when(service.getByIds(anyList())).thenReturn(List.of(prDTO1));
    configJwtMock(null, null, false, null);

    String uri = UriComponentsBuilder.fromUriString("/product/ids")
      .queryParam("list", List.of(prDTO1.getId()).toArray())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.GET, generateRequestWithToken(), ResponseDTO.class);
    System.out.println(response.toString());
    ResponseDTO responseDTO = response.getBody();
    List<ProductDTO> actualData = objectMapper
      .convertValue(responseDTO.getData(), new TypeReference<List<ProductDTO>>() {});

    assertEquals(200, responseDTO.getStatus());
    assertNull(responseDTO.getError());
    assertNotNull(responseDTO.getData());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void search() {
    when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of(prDTO1));
    configJwtMock(null, null, false, null);

    String uri = UriComponentsBuilder.fromUriString("/product/search")
      .queryParam("brand", "Marca 1")
      .queryParam("name", "Ventilador")
      .queryParam("categoryNames", "cat1")
      .queryParam("accountId", acc1ID.toString())
      .toUriString();

    ResponseDTO response = restTemplate.getForObject(uri, ResponseDTO.class);
    List<ProductDTO> actualData = objectMapper.convertValue(response.getData(), new TypeReference<List<ProductDTO>>() {});

    assertEquals(200, response.getStatus());
    assertNull(response.getError());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void create() {
    when(service.create(any())).thenReturn(prDTO1);

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1);
    ResponseDTO response = restTemplate.postForObject("/product", request, ResponseDTO.class);
    ProductDTO actual = objectMapper.convertValue(response.getData(), ProductDTO.class);

    assertEquals(201, response.getStatus());
    assertNull(response.getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void update() {
    when(service.update(any())).thenReturn(prDTO1);

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/product", HttpMethod.PUT, request, ResponseDTO.class);
    ProductDTO actual = objectMapper.convertValue(response.getBody().getData(), ProductDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void deleteById() {
    String uri = "/product?id=" + prDTO1.getId();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertEquals("Producto eliminado con éxito", response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByAccountId() {
    String uri = "/product/delete_by_account?id=" + acc1ID;

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByIds() {
    String uri = UriComponentsBuilder.fromUriString("/product/delete_by_ids")
      .queryParam("ids", prDTO1.getId())
      .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }
}
