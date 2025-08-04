package com.nahuelgg.inventory_app.users.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting.AuthData;
import com.nahuelgg.inventory_app.users.services.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthenticateControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;
  @Autowired AuthenticationForTesting authenticator;
  @Autowired JwtService jwtService;
  @Autowired BCryptPasswordEncoder encoder;

  @Autowired AccountRepository accountRepository;
  @Autowired UserRepository userRepository;

  String accUsername = "username";
  String accPassword = "password";
  String userName = "subUser";
  String userPassword = "userPassword";

  private HttpHeaders generateHeaderWithToken(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  private void registerAccountWithUser() {
    AccountEntity registeredAccount = accountRepository.save(AccountEntity.builder()
      .username(accUsername)
      .password(encoder.encode(accPassword))
      .users(new ArrayList<>())
      .inventoriesReferences(new ArrayList<>())
    .build());

    userRepository.save(UserEntity.builder()
      .name(userName)
      .password(encoder.encode(userPassword))
      .role("role")
      .associatedAccount(registeredAccount)
      .isAdmin(false)
      .inventoryPerms(new ArrayList<>())
    .build());
  }

  @Test
  void loginAccount_successWithoutToken() {
    registerAccountWithUser();
    LoginDTO input = new LoginDTO(accUsername, accPassword);

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input);
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/account", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertTrue(jwtService.isTokenValid(response.getBody().getToken(), accUsername));
  }

  @Test
  void loginAccount_successWithEmptyToken() {
    registerAccountWithUser();
    LoginDTO input = new LoginDTO(accUsername, accPassword);

    String emptyToken = jwtService.generateEmptyToken();
    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken(emptyToken));
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/account", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertTrue(jwtService.isTokenValid(response.getBody().getToken(), accUsername));
  }

  @Test
  void loginAsUser_success() {
    // Primero se loguea la cuenta únicamente, y luego se crea un usuario asociado a esa cuenta sin loguearlo todavía
    AuthData authData = authenticator.authenticate(new LoginDTO(accUsername, accPassword));
    AccountEntity loggedAccount = authData.getAccountSaved();
    userRepository.save(UserEntity.builder()
      .name(userName)
      .password(encoder.encode(userPassword))
      .role("role")
      .associatedAccount(loggedAccount)
      .isAdmin(false)
      .inventoryPerms(new ArrayList<>())
    .build());

    // Preparación del llamado para loguear al usuario creado
    LoginDTO input = new LoginDTO(userName, userPassword);

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken(authData.getToken()));
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/user", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertTrue(jwtService.isTokenValid(response.getBody().getToken(), accUsername));


  }

  @Test
  void loginAsUser_deniedIfNotLogged() {
    LoginDTO input = new LoginDTO(userName, userPassword);

    HttpEntity<LoginDTO> httpEntity = new HttpEntity<LoginDTO>(input, generateHeaderWithToken(jwtService.generateEmptyToken()));
    ResponseEntity<TokenDTO> response = restTemplate.exchange("/authenticate/login/user", HttpMethod.POST, httpEntity, TokenDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void logout_success() {
    AuthData authData = authenticator.authenticate(new LoginDTO(accUsername, accPassword));

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/account", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken(authData.getToken())), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());


  }

  @Test
  void logout_deniedIfNotLogged() {
    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/account", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken(jwtService.generateEmptyToken())), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void logoutUser_success() {
    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO(accUsername, accPassword),
      new LoginDTO(userName, userPassword),
      "role", null
    );

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/user", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken(authData.getToken())), TokenDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  @Test
  void logoutUser_deniedIfNotLogged() {
    AuthData authData = authenticator.authenticate(new LoginDTO(accUsername, accPassword));

    ResponseEntity<TokenDTO> response = restTemplate.exchange(
      "/authenticate/logout/user", HttpMethod.POST, 
      new HttpEntity<>(generateHeaderWithToken(authData.getToken())), TokenDTO.class
    );
    System.out.println(response.toString());
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }
}
