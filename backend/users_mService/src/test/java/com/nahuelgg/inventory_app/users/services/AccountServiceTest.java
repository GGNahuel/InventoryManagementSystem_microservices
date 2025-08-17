package com.nahuelgg.inventory_app.users.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.AccountRegistrationDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.dtos.UserRegistrationDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.enums.Permissions;
import com.nahuelgg.inventory_app.users.exceptions.EmptyFieldException;
import com.nahuelgg.inventory_app.users.exceptions.InvalidValueException;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.implementations.AccountService_Impl;
import com.nahuelgg.inventory_app.users.utilities.DTOMappers;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
  @Mock AccountRepository repository;
  @Mock UserRepository userRepository;
  @Mock InventoryRefRepository invRefRepository;
  @Mock PermissionsForInventoryRepository permsRepository;
  @Mock RestTemplate restTemplate;
  @Mock HttpGraphQlClient graphQLClient;
  @Mock BCryptPasswordEncoder encoder;
  @Mock DTOMappers dtoMappers;

/*   @Mock WebClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock WebClient.RequestBodySpec requestBodySpec;
  @Mock WebClient.ResponseSpec responseSpec; */

  @InjectMocks AccountService_Impl service;

  private AccountEntity acc;
  private AccountDTO accDTO;

  @BeforeEach
  void beforeEach() {
    acc = AccountEntity.builder()
      .id(UUID.randomUUID())
      .username("account")
      .password("456")
      .users(new ArrayList<>())
    .build();
    accDTO = AccountDTO.builder()
      .id(acc.getId().toString())
      .username("account")
      .users(new ArrayList<>())
    .build();
  }

  @Test
  void getById() {
    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));

    assertEquals(accDTO, service.getById(acc.getId()));
  }

  @Test
  void getById_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.getById(null));
  }

  @Test
  void getById_throwsResourceNotFound() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.getById(UUID.randomUUID()));
  }

  @Test
  void create() {
    String username = "account";
    String password = "pass";
    String passwordRepeated = "pass";
    String adminPassword = "admin";
    String adminPasswordRepeated = "admin";

    UserEntity savedAdmin = UserEntity.builder()
      .id(UUID.randomUUID())
      .name("admin")
      .password("encrypted")
      .role("admin")
      .isAdmin(true)
    .build();

    when(userRepository.save(any())).thenReturn(savedAdmin);
    when(repository.save(any())).thenReturn(acc);
    when(encoder.encode(anyString())).thenReturn("encrypted");

    AccountDTO result = service.create(AccountRegistrationDTO.builder()
      .username(username)
      .password(password)
      .passwordRepeated(passwordRepeated)
      .adminPassword(adminPassword)
      .adminPasswordRepeated(adminPasswordRepeated)
    .build());

    assertEquals("account", result.getUsername());
    assertNotNull(result.getId());
  }

  @Test
  void create_throwsEmptyFieldInImportantOnes() {
    assertThrows(EmptyFieldException.class, () -> service.create(new AccountRegistrationDTO("user", null, "password", "", "123", "123")));
    assertThrows(EmptyFieldException.class, () -> service.create(new AccountRegistrationDTO("", "password", "password", "", "123", "123")));
    assertThrows(EmptyFieldException.class, () -> service.create(new AccountRegistrationDTO("user", "password", "password", "", null, "123")));
  }

  @Test
  void create_throwsInvalidValue() {
    assertThrows(InvalidValueException.class, () ->
      service.create(new AccountRegistrationDTO("user", "123", "456", "", "admin", "admin"))
    );
    assertThrows(InvalidValueException.class, () ->
      service.create(new AccountRegistrationDTO("user", "123", "123", "", "admin", "456"))
    );
  }

  @Test
  void addUser() {
    String password = "1234";
    
    InventoryRefEntity invRef = InventoryRefEntity.builder()
      .id(UUID.randomUUID())
      .inventoryIdReference(UUID.randomUUID())
    .build();
    PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder()
      .permissions(List.of(Permissions.editProducts))
      .idOfInventoryReferenced(invRef.getInventoryIdReference().toString())
    .build();

    UserRegistrationDTO userInput = UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password(password)
      .passwordRepeated(password)
      .inventoryPerms(List.of(perm))
    .build();

    PermissionsForInventoryEntity mappedPerm = PermissionsForInventoryEntity.builder()
      .permissions("editProducts")
      .inventoryReference(invRef)
    .build();
    UserEntity mappedInput_savedEntity = UserEntity.builder()
      .id(UUID.randomUUID())
      .name("Juan")
      .role("caja")
      .associatedAccount(acc)
      .inventoryPerms(List.of(mappedPerm))
    .build();

    UserDTO expected = UserDTO.builder()
      .id(mappedInput_savedEntity.getId().toString())
      .name(userInput.getName())
      .role(userInput.getRole())
      .inventoryPerms(userInput.getInventoryPerms())
    .build();

    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));

    when(dtoMappers.mapPerms(perm)).thenReturn(mappedPerm);

    when(permsRepository.save(mappedPerm)).thenReturn(mappedPerm);
    when(encoder.encode(password)).thenReturn("encrypted-password");
    when(userRepository.save(any(UserEntity.class))).thenReturn(mappedInput_savedEntity);

    UserDTO actual = service.addUser(acc.getId(), userInput);

    assertEquals(expected, actual);
  }

  @Test
  void addUser_throwsEmptyFieldInImportantOnes() {
    UserRegistrationDTO completeOne =  UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("1234")
      .passwordRepeated("1234")
      .inventoryPerms(null)
    .build();
    UserRegistrationDTO withoutName = UserRegistrationDTO.builder()
      .name("")
      .role("caja")
      .password("1234")
      .passwordRepeated("1234")
      .inventoryPerms(null)
    .build();
    UserRegistrationDTO withoutRole = UserRegistrationDTO.builder()
      .name("Juan")
      .role(null)
      .password("1234")
      .passwordRepeated("1234")
      .inventoryPerms(null)
    .build();
    UserRegistrationDTO withoutPassword = UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("")
      .passwordRepeated("1234")
      .inventoryPerms(null)
    .build();
    UserRegistrationDTO withoutPasswordRepeated = UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("1234")
      .passwordRepeated(null)
      .inventoryPerms(null)
    .build();
    UserRegistrationDTO withoutInvRefInPerm = UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("1234")
      .passwordRepeated("1234")
      .inventoryPerms(List.of(
        PermissionsForInventoryDTO.builder()
          .permissions(List.of(Permissions.editInventory))
          .idOfInventoryReferenced(null)
        .build()
      ))
    .build();
    UserRegistrationDTO withoutPermsInPerm = UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("1234")
      .passwordRepeated("1234")
      .inventoryPerms(List.of(
        PermissionsForInventoryDTO.builder()
          .permissions(List.of())
          .idOfInventoryReferenced("id")
        .build()
      ))
    .build();

    assertAll(
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(null, completeOne)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutName)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutRole)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutPassword)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutPasswordRepeated)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutInvRefInPerm)),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(UUID.randomUUID(), withoutPermsInPerm))
    );
    verify(repository, never()).findById(any(UUID.class));
    verify(permsRepository, never()).save(any());
    verify(userRepository, never()).save(any());
    verify(repository, never()).save(any());
  }

  @Test
  void addUser_throwsInvalidField() {
    UserRegistrationDTO wrongPasswords =  UserRegistrationDTO.builder()
      .name("Juan")
      .role("caja")
      .password("1234")
      .passwordRepeated("4321")
      .inventoryPerms(null)
    .build();

    assertThrows(InvalidValueException.class, () ->
      service.addUser(acc.getId(), wrongPasswords)
    );
    verify(repository, never()).findById(any(UUID.class));
    verify(permsRepository, never()).save(any());
    verify(userRepository, never()).save(any());
    verify(repository, never()).save(any());
  }

  @Test
  void assignInventory_success() {
    UUID inventoryId = UUID.randomUUID();

    InventoryRefEntity savedRef = InventoryRefEntity.builder().id(UUID.randomUUID()).inventoryIdReference(inventoryId).build();

    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));
    when(invRefRepository.save(any())).thenReturn(savedRef);

    service.assignInventory(acc.getId(), inventoryId);

    verify(repository).findById(acc.getId());
    verify(invRefRepository).save(any(InventoryRefEntity.class));
  }

  @Test
  void assignInventory_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.assignInventory(null, UUID.randomUUID()));
    assertThrows(EmptyFieldException.class, () -> service.assignInventory(UUID.randomUUID(), null));
    verify(repository, never()).save(any());
  }

  @Test
  void removeInventoryAssigned() {
    UUID inventoryId = UUID.randomUUID();

    InventoryRefEntity invRef = InventoryRefEntity.builder()
      .id(UUID.randomUUID())
      .inventoryIdReference(inventoryId)
    .build();
    PermissionsForInventoryEntity perms = PermissionsForInventoryEntity.builder()
      .id(UUID.randomUUID())
      .inventoryReference(invRef)
    .build();
    AccountEntity accWithInv = acc.toBuilder()
      .inventoriesReferences(new ArrayList<>(List.of(invRef)))
    .build();

    when(repository.findById(acc.getId())).thenReturn(Optional.of(accWithInv));
    when(invRefRepository.findByInventoryIdReference(inventoryId)).thenReturn(Optional.of(invRef));
    when(permsRepository.findByReferencedInventoryId(inventoryId)).thenReturn(List.of(perms));

    service.removeInventoryAssigned(acc.getId(), inventoryId);

    verify(permsRepository).deleteAll(anyList());
    verify(repository).save(any());
  }

  @Test
  void removeInventoryAssigned_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.removeInventoryAssigned(UUID.randomUUID(), null));
    assertThrows(EmptyFieldException.class, () -> service.removeInventoryAssigned(null, UUID.randomUUID()));
    verify(repository, never()).save(any());
    verify(permsRepository, never()).deleteById(any());
  }

  @Test
  void delete() {
    String accId = accDTO.getId();
    String graphQlQuery = """
      mutation {
        deleteByAccountId(
          id: "%s"
        )
      }
    """.formatted(accId);

    GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
    GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);

    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));
    when(graphQLClient.document(graphQlQuery)).thenReturn(requestSpec);
    when(requestSpec.retrieve("deleteByAccountId")).thenReturn(retrieveSpec);
    when(retrieveSpec.toEntity(Boolean.class)).thenReturn(Mono.just(true));
    
    service.delete(acc.getId());

    verify(repository).findById(acc.getId());
    verify(graphQLClient).document(graphQlQuery);
    verify(restTemplate).delete("http://api-products:8081/product/delete-by-account?id=" + accId);
  }

  @Test
  void delete_doesNothingIfAccNotFound() {
    when(repository.findById(acc.getId())).thenReturn(Optional.empty());
    service.delete(acc.getId());

    verify(repository, never()).deleteById(acc.getId());
    verify(graphQLClient, never()).document(anyString());
    verify(restTemplate, never()).delete("http://api-products:8081/product/delete-by-account?id=" + acc.getId());
  }

  @Test
  void delete_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.delete(null));
  }
}
