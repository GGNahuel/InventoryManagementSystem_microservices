package com.nahuelgg.inventory_app.users.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.enums.Permissions;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting;
import com.nahuelgg.inventory_app.users.services.AuthenticationForTesting.AuthData;
import com.nahuelgg.inventory_app.users.utilities.DTOMappers;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;
  @Autowired DTOMappers dtoMappers;
  @Autowired AuthenticationForTesting authenticator;
  
  @Autowired UserRepository userRepository;
  @Autowired InventoryRefRepository invRefRepository;
  @Autowired PermissionsForInventoryRepository permsRepository;
  @Autowired AccountRepository accountRepository;

  @MockitoBean RestTemplate innerRestTemplate;
  @MockitoBean HttpGraphQlClient graphQlClient;

  EntityMappers eMappers = new EntityMappers();

  String token;

  private HttpHeaders generateHeaderWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  @Test
  void getById_successd() throws Exception {
    AuthData authData = authenticator.authenticate(new LoginDTO("username", "1234", false));
    AccountEntity loggedAccount = authData.getAccountSaved();
    token = authData.getToken();

    UserEntity newUser = userRepository.save(UserEntity.builder()
      .name("user")
      .password("1234")
      .role("role")
      .associatedAccount(loggedAccount)
      .isAdmin(false)
      .inventoryPerms(new ArrayList<>())
    .build());

    UserDTO expected = eMappers.mapUser(newUser);

    String url = UriComponentsBuilder.fromUriString("/user/id/" + newUser.getId().toString())
      .queryParam("accountId", loggedAccount.getId().toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class  
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(expected, actual);
  }

  @Test
  void getById_deniedCases()  {
    ResponseEntity<ResponseDTO> notAuthenticatedResponse = restTemplate.exchange(
      "/user/id/" + UUID.randomUUID() + "?accountId=" + UUID.randomUUID().toString(),
      HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class  
    );
    assertEquals(HttpStatusCode.valueOf(403), notAuthenticatedResponse.getStatusCode());
    
    token = authenticator.authenticate(new LoginDTO("loggedAccount", "1234", false)).getToken();
    ResponseEntity<ResponseDTO> notEqualsIdsResponse = restTemplate.exchange(
      "/user/id/" + UUID.randomUUID() + "?accountId=" + UUID.randomUUID().toString(),
      HttpMethod.GET,
      new HttpEntity<>(generateHeaderWithToken()),
      ResponseDTO.class  
    );
    assertEquals(HttpStatusCode.valueOf(403), notEqualsIdsResponse.getStatusCode());
  }

  @Test
  void edit_success() {
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("username", "1234", false));
    AccountEntity loggedAccount = authData.getAccountSaved();

    UserEntity userToEdit = userRepository.save(UserEntity.builder()
      .name("userToEdit")
      .password("1234")
      .role("role")
      .associatedAccount(loggedAccount)
      .inventoryPerms(new ArrayList<>())
      .isAdmin(false)
    .build());

    UserDTO userInput = UserDTO.builder()
      .id(userToEdit.getId().toString())
      .name("editedUser")
      .role("role")
    .build();
    UserDTO expected = eMappers.mapUser(userToEdit).toBuilder().name(userInput.getName()).build();

    token = authData.getToken();
    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(userInput, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/edit?accountId=" + loggedAccount.getId().toString(),
      HttpMethod.PUT, httpEntity,
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserDTO actual = objectMapper.convertValue(response.getBody().getData(), UserDTO.class);
    assertEquals(expected, actual);
    assertTrue(userRepository.findById(userToEdit.getId()).get().getName().equals(expected.getName()));
  }

  @Test
  void edit_deniedIfNotAdmin() {
    UserDTO userInput = UserDTO.builder()
      .id(UUID.randomUUID().toString())
      .name("editedUser")
      .role("role")
    .build();

    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234", false),
      new LoginDTO("loggedUser", "1234", false), 
      "notAdmin", null
    );

    token = authData.getToken();

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(userInput, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/edit?accountId=" + authData.getAccountSaved().getId().toString(),
      HttpMethod.PUT, httpEntity,
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void edit_deniedIfTryWithAnotherAccount() {
    UserDTO userInput = UserDTO.builder()
      .id(UUID.randomUUID().toString())
      .name("editedUser")
      .role("role")
    .build();

    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("accountUsername", "1234", false));

    token = authData.getToken();

    HttpEntity<UserDTO> httpEntity = new HttpEntity<UserDTO>(userInput, generateHeaderWithToken());
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/edit?accountId=" + UUID.randomUUID().toString(),
      HttpMethod.PUT, httpEntity,
      ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void assignNewPerms_success() {
    // Preparación de recursos
    InventoryRefEntity invRefSaved = invRefRepository.save(InventoryRefEntity.builder()
      .inventoryIdReference(UUID.randomUUID())
    .build());

    PermissionsForInventoryDTO input = PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invRefSaved.getInventoryIdReference().toString())
      .permissions(List.of(Permissions.editInventory))
    .build();
    
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("username", "1234", false));
    AccountEntity loggedAccount = authData.getAccountSaved();
    token = authData.getToken();

    UserEntity userToAssignPerm = userRepository.save(UserEntity.builder()
      .name("user")
      .password("1234")
      .role("role")
      .isAdmin(false)
      .inventoryPerms(new ArrayList<>())
      .associatedAccount(loggedAccount)
    .build());

    // Emulación del llamado al servicio de inventario, para que agregue usuario según la id de referencia del permiso
    GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
    GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.variables(anyMap())).thenReturn(requestSpec);
    when(requestSpec.retrieve("addUser")).thenReturn(retrieveSpec);
    when(retrieveSpec.toEntity(Boolean.class)).thenReturn(Mono.just(true));

    // Construcción de la solicitud y aserciones
    String url = UriComponentsBuilder.fromUriString("/user/add-perms")
      .queryParam("id", userToAssignPerm.getId().toString())
      .queryParam("accountId", loggedAccount.getId().toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH, 
      new HttpEntity<>(input, generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    UserEntity userWithPerm = userRepository.findById(userToAssignPerm.getId()).get();

    assertTrue(userWithPerm.getInventoryPerms().stream().anyMatch(
      invPermEntity -> invPermEntity.getPermissions().equals(
        dtoMappers.mapSpecificPermissions(input.getPermissions())
      )
    ));
    assertTrue(userWithPerm.getInventoryPerms().stream().anyMatch(
      invPermEntity -> invPermEntity.getInventoryReference().getInventoryIdReference().equals(
        UUID.fromString(input.getIdOfInventoryReferenced())
      )
    ));
  }

  @Test
  void assignNewPerms_deniedIfNotAdmin() throws Exception {
    PermissionsForInventoryDTO input = PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(UUID.randomUUID().toString())
      .permissions(List.of(Permissions.editInventory))
    .build();

    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234", false),
      new LoginDTO("loggedUser", "1234", false), 
      "notAdmin", null
    );
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/user/add-perms")
      .queryParam("id", UUID.randomUUID().toString())
      .queryParam("accountId", authData.getAccountSaved().getId().toString())
    .toUriString();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH, 
      new HttpEntity<>(input, generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void assignNewPerms_deniedIfTryWithAnotherAccount() {
    PermissionsForInventoryDTO input = PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(UUID.randomUUID().toString())
      .permissions(List.of(Permissions.editInventory))
    .build();

    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("username", "1234", false));
    token = authData.getToken();

    String url = UriComponentsBuilder.fromUriString("/user/add-perms")
      .queryParam("id", UUID.randomUUID().toString())
      .queryParam("accountId", UUID.randomUUID().toString())
    .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      url, HttpMethod.PATCH, 
      new HttpEntity<>(input, generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void delete_success() {
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("username", "1234", false));
    AccountEntity loggedAccount = authData.getAccountSaved();

    UserEntity userToDelete = userRepository.save(UserEntity.builder()
      .name("name")
      .password("1234")
      .role("role")
      .associatedAccount(loggedAccount)
      .isAdmin(false)
    .build());

    // Emulación del llamado al servicio de inventario, para que agregue usuario según la id de referencia del permiso
    GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
    GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);
    when(graphQlClient.document(anyString())).thenReturn(requestSpec);
    when(requestSpec.retrieve("removeUser")).thenReturn(retrieveSpec);
    when(retrieveSpec.toEntity(Boolean.class)).thenReturn(Mono.just(true));

    token = authData.getToken();
    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/delete?id=" + userToDelete.getId().toString() + "&accountId=" + loggedAccount.getId().toString(),
      HttpMethod.DELETE, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());
  }

  @Test
  void delete_deniedIfNotAdmin() {
    AuthData authData = authenticator.authenticateWithUserToo(
      new LoginDTO("accountUsername", "1234", false),
      new LoginDTO("loggedUser", "1234", false), 
      "notAdmin", null
    );
    token = authData.getToken();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/delete?id=" + UUID.randomUUID().toString() + "&accountId=" + authData.getAccountSaved().getId().toString(),
      HttpMethod.DELETE, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void delete_deniedIfTryWithAnotherAccount() {
    AuthData authData = authenticator.authenticateWithAdminToo(new LoginDTO("username", "1234", false));
    token = authData.getToken();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(
      "/user/delete?id=" + UUID.randomUUID().toString() + "&accountId=" + UUID.randomUUID().toString(),
      HttpMethod.DELETE, new HttpEntity<>(generateHeaderWithToken()), ResponseDTO.class
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }
}
