package com.nahuelgg.inventory_app.users.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.services.JwtService;
import com.nahuelgg.inventory_app.users.services.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test_UserController {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean UserService service;
  @MockitoBean JwtService jwtService;

  AccountDTO acc = AccountDTO.builder().id(UUID.randomUUID().toString()).username("accUsername").build();
  UserDTO user = UserDTO.builder().id(UUID.randomUUID().toString()).build();
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
  void getById_successIfAccountLogged() {
    when(service.getById(UUID.fromString(user.getId()))).thenReturn(user);
    configJwtMock(null, null, false, null, acc.getUsername());

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/id/" + user.getId(),
      HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class  
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(user, actual);
  }

  @Test
  void getById_deniedIfNotLoggedAccount() {
    configJwtMock(null, null, false, null, null);

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/id/" + user.getId(),
      HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class  
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).getById(any());
  }

  @Test
  void edit_successIfAdmin() {
    when(service.edit(user)).thenReturn(user);
    configJwtMock("admin", "admin", true, null, acc.getUsername());

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(user, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/user/edit", HttpMethod.PUT, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(user, actual);
  }

  @Test
  void edit_deniedIfNotAdmin() {
    configJwtMock("admin", "admin", false, null, acc.getUsername());

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(user, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/user/edit", HttpMethod.PUT, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).edit(any());
  }

  @Test
  void assignNewPerms_successIfAdmin() throws JsonProcessingException {
    PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder().id(UUID.randomUUID().toString()).build();
    when(service.assignNewPerms(perm, UUID.fromString(user.getId()))).thenReturn(user);
    configJwtMock("admin", "admin", true, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/user/add-perms")
      .queryParam("id", user.getId())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH, 
      new HttpEntity<>(perm, generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(user, actual);
  }

  @Test
  void assignNewPerms_deniedIfNotAdmin() throws JsonProcessingException {
    PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder().id(UUID.randomUUID().toString()).build();
    configJwtMock("admin", "admin", false, null, acc.getUsername());

    String url = UriComponentsBuilder.fromUriString("/user/add-perms")
      .queryParam("id", user.getId())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH, 
      new HttpEntity<>(perm, generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).assignNewPerms(any(), any());
  }

  @Test
  void delete_successIfAdmin() {
    configJwtMock("admin", "admin", true, null, acc.getUsername());

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/" + user.getId(), HttpMethod.DELETE,
      new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    verify(service).delete(UUID.fromString(user.getId()));
  }

  @Test
  void delete_deniedIfNotAdmin() {
    configJwtMock("admin", "admin", false, null, acc.getUsername());

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/" + user.getId(), HttpMethod.DELETE,
      new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(service, never()).delete(any());
  }
}
