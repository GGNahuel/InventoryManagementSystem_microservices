package com.nahuelgg.inventory_app.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.exceptions.EmptyFieldException;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.services.JwtService;
import com.nahuelgg.inventory_app.users.services.AccountService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExceptionHandlerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean JwtService jwtService;
  @MockitoBean AccountService service;

  String token = "abcde";
  String accId = "12345";
  HttpEntity<String> entity;

  @BeforeEach
  void configJwtMock() throws Exception {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject("accUsername");
      claims.put("accountId", accId);
      claims.put("userName", "userName");
      claims.put("userRole", "userRole");
      claims.put("isAdmin", false);
      claims.put("userPerms", List.of());
      return claimGetter.apply(claims);
    });
    when(jwtService.isTokenExpired(token)).thenReturn(false);
    when(jwtService.isTokenValid(token, "accUsername")).thenReturn(true);
    when(jwtService.mapTokenClaims(token)).thenReturn(JwtClaimsDTO.builder()
      .accountId(accId)
      .userName("userName")
      .userRole("userRole")
      .isAdmin(false)
      .userPerms(List.of())
    .build());

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    entity = new HttpEntity<>(headers);
  }

  @Test
  void emptyFieldEx() {
    when(service.getAll()).thenThrow(EmptyFieldException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account", HttpMethod.GET, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(406), response.getStatusCode());
  }

  @Test
  void resourceNotFound() {
    when(service.getAll()).thenThrow(ResourceNotFoundException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account", HttpMethod.GET, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }

  @Test
  void mismatchedType() throws JsonProcessingException {
    Map<String, Object> invalidInput = new HashMap<>();
    invalidInput.put("inventoryPerms", Map.of("foo", "bar"));

    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", accId)
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(invalidInput, headers);

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, postEntity, ResponseDTO.class);

    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
    assertTrue(response.getBody().getError().getExClass().contains("MismatchedInputException"));
  }

  @Test
  void paramTypeMismatch() {
    when(service.getAll()).thenThrow(MethodArgumentTypeMismatchException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account", HttpMethod.GET, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }

  @Test
  void missingParam() {
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account/register", HttpMethod.POST, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }

  @Test
  void accessDenied() {
    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", accId)
      .queryParam("invId", "invId-12345")
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity(url, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }
  
  @Test
  void globalException() {
    when(service.getAll()).thenThrow(RuntimeException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/product", HttpMethod.GET, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
  }
}
