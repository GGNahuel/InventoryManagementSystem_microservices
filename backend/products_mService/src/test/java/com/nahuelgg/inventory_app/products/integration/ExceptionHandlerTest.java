package com.nahuelgg.inventory_app.products.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.exceptions.EmptyFieldException;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.products.services.JwtService;
import com.nahuelgg.inventory_app.products.services.ProductService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExceptionHandlerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean JwtService jwtService;
  @MockitoBean ProductService service;

  String token = "abcde";
  String invId = "12345";
  String accountId = UUID.randomUUID().toString();

  private HttpHeaders generateHeaderWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  @BeforeEach
  void configJwtMock() throws Exception {
    when(jwtService.getClaim(eq(token), any())).thenReturn("accountUsername");
    when(jwtService.mapTokenClaims(token)).thenReturn(JwtClaimsDTO.builder()
      .accountId(accountId)
      .isAdmin(true)
      .userPerms(new ArrayList<>())
    .build());
    when(jwtService.isTokenExpired(token)).thenReturn(false);
  }

  @Test
  void emptyFieldEx() {
    when(service.getByIds(List.of())).thenThrow(EmptyFieldException.class);
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()),
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(406), response.getStatusCode());
  }

  @Test
  void resourceNotFound() {
    when(service.getByIds(List.of())).thenThrow(ResourceNotFoundException.class);
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()),
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }

  @Test
  void mismatchedType() throws JsonProcessingException {
    Map<String, Object> invalidInput = new HashMap<>();
    invalidInput.put("categories", Map.of("invalidObject", "forCategoriesParam"));
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(invalidInput, headers);

    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product?invId=" + invId, HttpMethod.POST, postEntity,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
    assertTrue(response.getBody().getError().getExClass().contains("MismatchedInputException"));
  }

  @Test
  void paramTypeMismatch() {
    when(service.getByIds(List.of())).thenThrow(MethodArgumentTypeMismatchException.class);
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()),
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }

  @Test
  void missingParam() {
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product/ids", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()),
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }

  @Test
  void accessDenied() {
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product", HttpMethod.GET, null,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }
  
  @Test
  void globalException() {
    when(service.getByIds(List.of())).thenThrow(RuntimeException.class);
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      "/product", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()),
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
  }
}
