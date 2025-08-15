package com.nahuelgg.inventory_app.users.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.AccountRegistrationDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting.AuthData;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AccountControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;
  @Autowired AuthenticationForTesting authenticator;

  @Autowired AccountRepository accountRepository;
  @Autowired UserRepository userRepository;
  @Autowired InventoryRefRepository inventoryRefRepository;

  @MockitoBean RestTemplate innerRestTemplate;
  @MockitoBean HttpGraphQlClient graphQlClient;

  EntityMappers eMappers = new EntityMappers();

  String token;

  private HttpHeaders generateHeaderWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  /* @Test estos tests se tendrían que adaptar para cuando se implemente el superAdmin
  @DirtiesContext
  void getAll_successIfAuthenticated() {
    accountRepository.save(AccountEntity.builder().username(exampleAcc.getUsername()).password("4321").build());
    
    AuthData authenticatedData = authenticator.authenticate(new LoginDTO("account", "1234", false));
    token = authenticatedData.getToken();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/account", HttpMethod.GET, 
      new HttpEntity<>(generateHeaderWithToken()), 
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    
    List<AccountDTO> actual = objectMapper.convertValue(
      response.getBody().getData(),
      new TypeReference<List<AccountDTO>>() {}
    );

    assertEquals(2, actual.size());
    assertTrue(actual.stream().anyMatch(accDto -> accDto.getUsername().equals(exampleAcc.getUsername())));
    assertTrue(actual.stream().anyMatch(accDto -> accDto.getUsername().equals("account")));
  }

  @Test
  void getAll_denied() {
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/account", HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  } */

  @Test
  @DirtiesContext
  void getById() {
    AuthData authData = authenticator.authenticate(new LoginDTO("user", "password"));
    AccountEntity loggedAccount = authData.getAccountSaved();
    
    AccountDTO expected = AccountDTO.builder()
      .id(loggedAccount.getId().toString())
      .username(loggedAccount.getUsername())
      .users(loggedAccount.getUsers().stream().map(
        userEntity -> eMappers.mapUser(userEntity)
      ).collect(Collectors.toList()))
      .inventoryReferenceIds(new ArrayList<>())
    .build();

    token = authData.getToken();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/account/id/" + expected.getId(), HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    AccountDTO actual = objectMapper.convertValue(response.getBody().getData(), AccountDTO.class);
    assertEquals(expected, actual);
  }

  @Test
  @DirtiesContext
  void getById_deniedCases() {
    AccountEntity anotherAccount = accountRepository.save(AccountEntity.builder()
      .username("anotherAccount")
      .password("1234")
    .build());

    ResponseEntity<ResponseDTO> notLoggedResponse = restTemplate.exchange(
      "/account/id/" + anotherAccount.getId().toString(), HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), notLoggedResponse.getStatusCode(), "No tendría que permitir si no hay autenticación");


    token = authenticator.authenticate(new LoginDTO("loggedAccount", "1234")).getToken();
    ResponseEntity<ResponseDTO> notEqualsIdsResponse = restTemplate.exchange(
      "/account/id/" + anotherAccount.getId().toString(), HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(
      HttpStatusCode.valueOf(403), notEqualsIdsResponse.getStatusCode(), 
      "No tendría que permitir si la id ingresada no corresponde a la cuenta que está en sesión"
    );
  }

  @Test
  @DirtiesContext
  void create_successEvenWithoutAuthentication() {
    String username = "user";
    String password = "password";
    String adminPassword = "adminPassword";

    HttpEntity<AccountRegistrationDTO> entity = new HttpEntity<>(new AccountRegistrationDTO(username, password, password, adminPassword, adminPassword));
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/account/register", HttpMethod.POST, entity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode());
    
    Optional<AccountEntity> accountSaved = accountRepository.findByUsername(username);
    assertTrue(accountSaved.isPresent());
    
    AccountEntity accountEntity = accountSaved.get();
    assertTrue(accountEntity.getUsername().equals(username));
    assertFalse(accountEntity.getPassword().equals(password));
    assertTrue(userRepository.findAll().size() == 1);
  }

  @Test
  @DirtiesContext
  void addUser_success() {
    UserDTO user = UserDTO.builder().name("user").role("role").build();
    
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("user", "1234"));
    AccountEntity loggedAccount = authData.getAccountSaved();
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", loggedAccount.getId().toString())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(user, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    
    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(user.getName(), actual.getName());
    assertEquals(user.getRole(), actual.getRole());
    assertTrue(userRepository.findAll().stream().anyMatch(
      userSaved -> userSaved.getName().equals(user.getName()) && userSaved.getAssociatedAccount().getId().equals(loggedAccount.getId())
    ));
  }

  @Test
  @DirtiesContext
  void addUser_deniedIfNotAdmin() {
    UserDTO inputUser = UserDTO.builder().name("user").role("role").build();

    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234"),
      new LoginDTO("loggedUser", "1234"), 
      "notAdmin", null
    );
    AccountEntity loggedAccount = authData.getAccountSaved();    
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", loggedAccount.getId().toString())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();
    
    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(inputUser, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    assertTrue(userRepository.findAll().stream().noneMatch(userSaved -> userSaved.getName().equals(inputUser.getName())));
  }

  @Test
  @DirtiesContext
  void addUser_deniedIfTryWithAnotherAccount() {
    UserDTO inputUser = UserDTO.builder().name("user").role("role").build();

    AccountEntity anotherAccount = accountRepository.save(AccountEntity.builder()
      .username("anotherAccount")
      .password("1234")
      .users(new ArrayList<>())
      .inventoriesReferences(new ArrayList<>())
    .build());

    AuthData authData = authenticator.authenticate(new LoginDTO("loggedAccount", "1234"));
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-user")
      .queryParam("accountId", anotherAccount.getId().toString())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(inputUser, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    assertTrue(userRepository.findAll().stream().noneMatch(userSaved -> userSaved.getName().equals(inputUser.getName())));
  }

  @Test
  @DirtiesContext
  void assignInventory_success() {
    UUID refInvId = UUID.randomUUID();

    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("account", "1234"));
    AccountEntity loggedAcc = authData.getAccountSaved();
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", authData.getAccountSaved().getId().toString())
      .queryParam("invRefId", refInvId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());

    Optional<AccountEntity> affectedAccount = accountRepository.findById(loggedAcc.getId());
    assertTrue(affectedAccount.isPresent());
    assertTrue(affectedAccount.get().getInventoriesReferences().stream().anyMatch(invRef -> invRef.getInventoryIdReference().equals(refInvId)));
  }

  @Test
  @DirtiesContext
  void assignInventory_deniedIfNotAdmin() {
    UUID refInvId = UUID.randomUUID();
    
    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234"),
      new LoginDTO("loggedUser", "1234"), 
      "notAdmin", null
    );
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", authData.getAccountSaved().getId().toString())
      .queryParam("invRefId", refInvId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    assertTrue(inventoryRefRepository.findAll().isEmpty());
  }

  @Test
  @DirtiesContext
  void assignInventory_deniedIfTryWithAnotherAccount() {
    UUID refInvId = UUID.randomUUID();
    AccountEntity anotherAccount = accountRepository.save(AccountEntity.builder()
      .username("anotherAccount")
      .password("1234")
      .users(new ArrayList<>())
      .inventoriesReferences(new ArrayList<>())
    .build());
    
    token = authenticator.authenticate(new LoginDTO("loggedAccount", "1234")).getToken();

    String url = UriComponentsBuilder.fromUriString("/account/add-inventory")
      .queryParam("accountId", anotherAccount.getId().toString())
      .queryParam("invRefId", refInvId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    assertTrue(inventoryRefRepository.findAll().isEmpty());
  }

  @Test
  @DirtiesContext
  void removeInventoryAssigned_success() {
    UUID invId = UUID.randomUUID();
    InventoryRefEntity invToRemove = inventoryRefRepository.save(InventoryRefEntity.builder()
      .inventoryIdReference(invId)
    .build());
    
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("account", "1234"));
    AccountEntity loggedAcc = authData.getAccountSaved();
    token = authData.getToken();

    List<InventoryRefEntity> invRefsInLoggedAccount = new ArrayList<>();
    invRefsInLoggedAccount.add(invToRemove);
    loggedAcc.setInventoriesReferences(invRefsInLoggedAccount);
    accountRepository.save(loggedAcc);

    String url = UriComponentsBuilder.fromUriString("/account/remove-inventory")
      .queryParam("accountId",loggedAcc.getId().toString())
      .queryParam("invRefId", invId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());

    Optional<AccountEntity> affectedAccount = accountRepository.findById(loggedAcc.getId());
    assertTrue(affectedAccount.isPresent());
    assertTrue(affectedAccount.get().getInventoriesReferences().isEmpty());
  }

  @Test
  @DirtiesContext
  void removeInventoryAssigned_deniedIfNotAdmin() {
    UUID refInvId = UUID.randomUUID();
    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234"),
      new LoginDTO("loggedUser", "1234"), 
      "notAdmin", null
    );
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/remove-inventory")
      .queryParam("accountId", authData.getAccountSaved().getId().toString())
      .queryParam("invRefId", refInvId.toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void removeInventoryAssigned_deniedIfTryWithAnotherAccount() {
    UUID refInvId = UUID.randomUUID();
    AccountEntity anotherAccount = accountRepository.save(AccountEntity.builder()
      .username("anotherAccount")
      .password("1234")
      .users(new ArrayList<>())
      .inventoriesReferences(new ArrayList<>())
    .build());

    token = authenticator.authenticate(new LoginDTO("accountUsername", "1234")).getToken();

    String url = UriComponentsBuilder.fromUriString("/account/remove-inventory")
      .queryParam("accountId", anotherAccount.getId().toString())
      .queryParam("invRefId", refInvId.toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void delete() {
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("account", "1234"));
    AccountEntity loggedAccount = authData.getAccountSaved();
    token = authData.getToken();

    String graphQlQuery = """
      mutation {
        deleteByAccountId(
          id: "%s"
        )
      }
    """.formatted(loggedAccount.getId());

    GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
    GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);
    when(graphQlClient.document(graphQlQuery)).thenReturn(requestSpec);
    when(requestSpec.retrieve("deleteByAccountId")).thenReturn(retrieveSpec);
    when(retrieveSpec.toEntity(Boolean.class)).thenReturn(Mono.just(true));

    String url = UriComponentsBuilder.fromUriString("/account/delete")
      .queryParam("id", loggedAccount.getId().toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.DELETE, 
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertTrue(accountRepository.findById(loggedAccount.getId()).isEmpty());
    verify(innerRestTemplate).delete(
      "http://api-products:8081/product/delete-by-account?id=" + loggedAccount.getId().toString()
    );
    verify(graphQlClient).document(graphQlQuery);
  }

  @Test
  @DirtiesContext
  void delete_deniedIfNotAdmin() {
    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234"),
      new LoginDTO("loggedUser", "1234"), 
      "notAdmin", null
    );
    AccountEntity loggedAccount = authData.getAccountSaved();
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/delete")
      .queryParam("id", loggedAccount.getId().toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.DELETE,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(graphQlClient, never()).document(anyString());
    verify(innerRestTemplate, never()).delete(anyString());
  }

  @Test
  void delete_deniedIfTryWithAnotherAccount() {
    AccountEntity anotherAccount = accountRepository.save(AccountEntity.builder()
      .username("anotherAccount")
      .password("1234")
    .build());

    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("account", "1234"));
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/account/delete")
      .queryParam("id", anotherAccount.getId().toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.DELETE, 
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());

    verify(graphQlClient, never()).document(anyString());
    verify(innerRestTemplate, never()).delete(anyString());
  }
}
