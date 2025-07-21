package com.nahuelgg.inventory_app.users.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.services.AuthenticationService;
import com.nahuelgg.inventory_app.users.services.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthenticateControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean AuthenticationService authenticationService;
  @MockitoBean JwtService jwtService;

  AccountDTO acc = AccountDTO.builder().id(UUID.randomUUID().toString()).username("accUsername").build();
  String token = "tokenTest";

  private void configJwtMock(String userName, String userRole, boolean isAdmin, List<PermissionsForInventoryDTO> userPerms, String accLoggedUsername) {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject(accLoggedUsername);
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
  void loginAccount_successWithoutToken() {
    LoginDTO input = LoginDTO.builder().accountLogin(true).username(acc.getUsername()).password("123test").build();
    when(authenticationService.login(input)).thenReturn(new TokenDTO(token));

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input);
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/account", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    verify(authenticationService).login(input);
  }

  @Test
  void loginAccount_successWithEmptyToken() {
    LoginDTO input = LoginDTO.builder().accountLogin(true).username(acc.getUsername()).password("123test").build();
    when(authenticationService.login(input)).thenReturn(new TokenDTO(token));
    configJwtMock(null, null, false, null, null);

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken());
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/account", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    verify(authenticationService).login(input);
  }

  @Test
  void loginAsUser_success() {
    LoginDTO input = LoginDTO.builder().accountLogin(false).username("subUser").password("123test").build();
    when(authenticationService.loginAsUser(input)).thenReturn(new TokenDTO(token));
    configJwtMock("subUser", "role", false, null, acc.getUsername());

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken());
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/user", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    verify(authenticationService).loginAsUser(input);
  }

  @Test
  void loginAsUser_deniedIfNotLogged() {
    LoginDTO input = LoginDTO.builder().accountLogin(false).username("subUser").password("123test").build();
    configJwtMock(null, null, false, null, null);

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken());
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/user", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(authenticationService, never()).loginAsUser(input);
  }

  @Test
  void logout_success() {
    when(authenticationService.logout()).thenReturn(new TokenDTO(token));
    configJwtMock("subUser", "role", false, null, acc.getUsername());

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/account", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken()), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    verify(authenticationService).logout();
  }

  @Test
  void logout_deniedIfNotLogged() {
    configJwtMock("subUser", "role", false, null, null);

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/account", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken()), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(authenticationService, never()).logout();
  }

  @Test
  void logoutUser_success() {
    when(authenticationService.logoutAsUser()).thenReturn(new TokenDTO(token));
    configJwtMock("subUser", "role", false, null, acc.getUsername());

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/user", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken()), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    verify(authenticationService).logoutAsUser();
  }

  @Test
  void logoutUser_deniedIfNotLogged() {
    configJwtMock("subUser", "role", false, null, null);

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/account", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken()), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(authenticationService, never()).logoutAsUser();
  }
}
