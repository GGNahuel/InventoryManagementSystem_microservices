package com.nahuelgg.inventory_app.users.servicers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
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

@ExtendWith(MockitoExtension.class)
public class Test_AccountService {
  @Mock AccountRepository repository;
  @Mock UserRepository userRepository;
  @Mock InventoryRefRepository invRefRepository;
  @Mock PermissionsForInventoryRepository permsRepository;
  @Mock RestTemplate restTemplate;
  @Mock WebClient webClient;
  @Mock BCryptPasswordEncoder encoder;
  @Mock DTOMappers dtoMappers;

/*   @Mock WebClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock WebClient.RequestBodySpec requestBodySpec;
  @Mock WebClient.ResponseSpec responseSpec; */

  @InjectMocks AccountService_Impl service;

  private AccountEntity acc;
  private AccountDTO accDTO;
  private UserDTO userDTO;

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

    InventoryRefEntity invRef = InventoryRefEntity.builder()
      .id(UUID.randomUUID())
      .inventoryIdReference(UUID.randomUUID())
    .build();
    PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder()
      .id(UUID.randomUUID().toString())
      .permissions(List.of(Permissions.editProducts))
      .idOfInventoryReferenced(invRef.getInventoryIdReference().toString())
    .build();
    userDTO = UserDTO.builder()
      .name("Pablo")
      .role("gerente de logística")
      .inventoryPerms(List.of(perm))
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

    AccountDTO result = service.create(username, password, passwordRepeated, adminPassword, adminPasswordRepeated);

    assertEquals(accDTO, result);
  }

  @Test
  void create_throwsEmptyFieldInImportantOnes() {
    assertThrows(EmptyFieldException.class, () -> service.create("user", null, "password", "123", "123"));
    assertThrows(EmptyFieldException.class, () -> service.create("", "password", "password", "123", "123"));
    assertThrows(EmptyFieldException.class, () -> service.create("user", "password", "password", null, "123"));
  }

  @Test
  void create_throwsInvalidValue() {
    assertThrows(InvalidValueException.class, () ->
      service.create("user", "123", "456", "admin", "admin")
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
      .id(UUID.randomUUID().toString())
      .permissions(List.of(Permissions.editProducts))
      .idOfInventoryReferenced(invRef.getInventoryIdReference().toString())
    .build();
    UserDTO userInput = UserDTO.builder()
      .name("Juan")
      .role("caja")
      .inventoryPerms(List.of(perm))
    .build();

    PermissionsForInventoryEntity mappedPerm = PermissionsForInventoryEntity.builder()
      .id(UUID.fromString(perm.getId()))
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

    UserDTO expected = userInput.toBuilder()
      .id(mappedInput_savedEntity.getId().toString())
    .build();

    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));

    when(dtoMappers.mapUser(userInput, acc)).thenReturn(mappedInput_savedEntity);
    when(dtoMappers.mapPerms(perm)).thenReturn(mappedPerm);

    when(permsRepository.save(mappedPerm)).thenReturn(mappedPerm);
    when(encoder.encode(password)).thenReturn("encrypted-password");
    when(userRepository.save(any(UserEntity.class))).thenReturn(mappedInput_savedEntity);
    when(repository.save(any(AccountEntity.class))).thenReturn(acc);

    UserDTO actual = service.addUser(userInput, acc.getId(), password, password);

    assertEquals(expected, actual);
  }

  @Test
  void addUser_throwsEmptyFieldInImportantOnes() {
    UserDTO withoutName = userDTO.toBuilder().name("").build();
    UserDTO withoutRole = userDTO.toBuilder().role(null).build();
    UserDTO withoutInvRefInPerm =userDTO.toBuilder()
      .inventoryPerms(List.of(
        PermissionsForInventoryDTO.builder()
          .permissions(List.of(Permissions.editInventory))
          .idOfInventoryReferenced(null)
        .build()
      ))
    .build();
    UserDTO withoutPermsInPerm =userDTO.toBuilder()
      .inventoryPerms(List.of(
        PermissionsForInventoryDTO.builder()
          .permissions(List.of())
          .idOfInventoryReferenced("id")
        .build()
      ))
    .build();

    assertAll(
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(userDTO, null, "null", "null")),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(userDTO, UUID.randomUUID(), null, "null")),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(withoutName, UUID.randomUUID(), "null", "null")),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(withoutRole, UUID.randomUUID(), "null", "null")),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(withoutInvRefInPerm, UUID.randomUUID(), "null", "null")),
      () -> assertThrows(EmptyFieldException.class, () -> service.addUser(withoutPermsInPerm, UUID.randomUUID(), "null", "null"))
    );
    verify(repository, never()).findById(any(UUID.class));
    verify(permsRepository, never()).save(any());
    verify(userRepository, never()).save(any());
    verify(repository, never()).save(any());
  }

  @Test
  void addUser_throwsInvalidField() {
    assertThrows(InvalidValueException.class, () ->
      service.addUser(userDTO, acc.getId(), "pass1", "pass2")
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
    AccountDTO expected = accDTO.toBuilder()
      .idsOfInventoryReferred(List.of(inventoryId.toString()))
    .build();

    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));
    when(invRefRepository.save(any())).thenReturn(savedRef);
    when(repository.save(any())).thenReturn(acc);

    AccountDTO result = service.assignInventory(acc.getId(), inventoryId);

    assertEquals(expected, result);
    verify(repository, times(1)).save(any());
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

    verify(permsRepository).deleteById(perms.getId());
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
    /* averiguar cómo se testean las llamadas con webClient transformadas a comunicación sincróna
    when(repository.findById(acc.getId())).thenReturn(Optional.of(acc));
    when(restTemplate.exchange(
      eq("http://api_products/product/delete_by_account?id=" + acc.getId()),
      eq(HttpMethod.DELETE),
      any(),
      eq(Void.class)
    )).thenReturn(ResponseEntity.ok().build());

    // request to inventories microservices
    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri("/")).thenReturn(requestBodySpec);
    when(requestBodySpec.bodyValue(any())).thenReturn(responseSpec);
    when(responseSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

    service.delete(acc.getId());

    verify(repository).deleteById(acc.getId()); */
  }

  @Test
  void delete_doesNothingIfAccNotFound() {
    when(repository.findById(acc.getId())).thenReturn(Optional.empty());
    service.delete(acc.getId());
    verify(repository, never()).deleteById(acc.getId());
  }

  @Test
  void delete_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.delete(null));
  }

  @Test
  void loadUserByUsername_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.loadUserByUsername(""));
  }

  @Test
  void loadUserByUsername_throwsResourceNotFound() {
    when(repository.findByUsername(any())).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.loadUserByUsername("notFound"));
  }
}
