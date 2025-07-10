package com.nahuelgg.inventory_app.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.services.AccountService;
import com.nahuelgg.inventory_app.users.services.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AccountControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean AccountService service;
  @MockitoBean JwtService jwtService;

  AccountDTO acc = AccountDTO.builder()
    .id(UUID.randomUUID().toString())
    .username("account")
    .users(new ArrayList<>())
  .build();

  String token = "testToken";

  private void configJwtMock(String userName, String userRole, boolean isAdmin, List<PermissionsForInventoryDTO> userPerms, String accUsername) {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject(accUsername);
      claims.put("accountId", acc.getId());
      claims.put("userName", userName);
      claims.put("userRole", userRole);
      claims.put("isAdmin", isAdmin);
      claims.put("userPerms", userPerms);
      return claimGetter.apply(claims);
    });
    when(jwtService.isTokenExpired(token)).thenReturn(false);
    when(jwtService.isTokenValid(token, acc.getUsername())).thenReturn(true);
    when(jwtService.mapTokenClaims(token)).thenReturn(JwtClaimsDTO.builder()
      .accountId(acc.getId())
      .userName(userName)
      .userRole(userRole)
      .isAdmin(isAdmin)
      .userPerms(userPerms != null ? userPerms : List.of())
    .build());
  }

  private HttpHeaders generateHeaderWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  @Test
  void getAll_successIfAuthenticated() {
    List<AccountDTO> expected = List.of(acc);
    when(service.getAll()).thenReturn(expected);
    configJwtMock(null, null, false, null, acc.getUsername());

    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    List<AccountDTO> actual = objectMapper.convertValue(
      response.getBody().getData(),
      new TypeReference<List<AccountDTO>>() {}
    );

    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertIterableEquals(expected, actual);
  }

  @Test
  void getAll_denied() {
    configJwtMock(null, null, false, null, null);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account", HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void getById() {
    when(service.getById(UUID.fromString(acc.getId()))).thenReturn(acc);
    configJwtMock(null, null, false, null, acc.getUsername());

    HttpEntity<String> httpEntity = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account/id/" + acc.getId(), HttpMethod.GET, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    AccountDTO actual = objectMapper.convertValue(response.getBody().getData(), AccountDTO.class);
    assertEquals(acc, actual);
  }

  @Test
  void getById_denied() {
    configJwtMock(null, null, false, null, null);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account/id/" + acc.getId(), HttpMethod.GET, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void create_successEvenWithoutAuthentication() {
    String username = "user";
    String password = "password";
    String adminPassword = "adminPassword";
    when(service.create(username, password, password, adminPassword, adminPassword)).thenReturn(acc);

    String url = UriComponentsBuilder.fromUriString("/account/register")
      .queryParam("username", username)
      .queryParam("password", password)
      .queryParam("passwordRepeated", password)
      .queryParam("adminPassword", adminPassword)
      .queryParam("adminPasswordRepeated", adminPassword)
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, null, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode());
    
    AccountDTO actual = objectMapper.convertValue(response.getBody().getData(), AccountDTO.class);
    assertEquals(acc, actual);
  }

  @Test
  void addUser_success() {
    UserDTO user = UserDTO.builder().name("user").build();
    when(service.addUser(user, UUID.fromString(acc.getId()), "123", "123")).thenReturn(user);
    configJwtMock("admin", "admin", true, null, acc.getUsername());
    
    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", acc.getId())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(user, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    
    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(user, actual);
  }

  @Test
  void addUser_deniedIfNotAdmin() {
    configJwtMock("user", "role", false, null, acc.getUsername());
    UserDTO user = UserDTO.builder().name("user").build();
    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", acc.getId())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();
    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(user, generateHeaderWithToken());

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).addUser(any(), any(), any(), any());
  }

  @Test
  void assignInventory_success() {
    UUID invId = UUID.randomUUID();
    configJwtMock("admin", "admin", true, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    verify(service).assignInventory(UUID.fromString(acc.getId()), invId);
  }

  @Test
  void assignInventory_deniedIfNotAdmin() {
    UUID invId = UUID.randomUUID();
    configJwtMock("user", "role", false, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).assignInventory(any(), any());
  }

  @Test
  void removeInventoryAssigned() {
    UUID invId = UUID.randomUUID();
    configJwtMock("user", "role", true, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/remove-inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    verify(service).removeInventoryAssigned(UUID.fromString(acc.getId()), invId);
  }

  @Test
  void removeInventoryAssigned_deniedIfNotAdmin() {
    UUID invId = UUID.randomUUID();
    configJwtMock("user", "role", false, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/remove-inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).removeInventoryAssigned(any(), any());
  }

  @Test
  void delete() {
    configJwtMock("user", "role", true, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/delete")
      .queryParam("id", acc.getId())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    verify(service).delete(UUID.fromString(acc.getId()));
  }

  @Test
  void delete_deniedIfNotAdmin() {
    configJwtMock("user", "role", false, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/account/delete")
      .queryParam("id", acc.getId())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).delete(any());
  }
}
