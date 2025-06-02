package com.nahuelgg.inventory_app.users.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.enums.Permissions;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;

@ExtendWith(MockitoExtension.class)
public class Test_DTOMappers {
  @Mock InventoryRefRepository inventoryRefRepository;
  @InjectMocks DTOMappers dtoMappers;

  InventoryRefEntity invRef = InventoryRefEntity.builder()
    .id(UUID.randomUUID())
    .inventoryIdReference(UUID.randomUUID())
  .build();
  PermissionsForInventoryEntity permE = PermissionsForInventoryEntity.builder()
    .id(UUID.randomUUID())
    .permissions("editProducts,addProducts")
    .inventoryReference(invRef)
  .build();
  UserEntity userE = UserEntity.builder()
    .id(UUID.randomUUID())
    .name("Juan")
    .role("caja")
    .inventoryPerms(List.of(permE))
  .build();

  PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder()
    .id(permE.getId().toString())
    .permissions(List.of(Permissions.editProducts, Permissions.addProducts))
    .idOfInventoryReferenced(invRef.getInventoryIdReference().toString())
  .build();
  UserDTO userDTO = UserDTO.builder()
    .id(userE.getId().toString())
    .name("Juan")
    .role("caja")
    .inventoryPerms(List.of(perm))
  .build();

  @BeforeEach
  void beforeEach() {
    when(inventoryRefRepository.findByInventoryIdReference(invRef.getInventoryIdReference())).thenReturn(Optional.of(invRef));
  }

  @Test
  void mapInventoryRef() {
    assertEquals(invRef, dtoMappers.mapInventoryRef(invRef.getInventoryIdReference().toString()));
    verify(inventoryRefRepository).findByInventoryIdReference(invRef.getInventoryIdReference());
  }

  @Test
  void mapInventoryRef_throwsResourceNotFound() {
    when(inventoryRefRepository.findByInventoryIdReference(invRef.getInventoryIdReference())).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> dtoMappers.mapInventoryRef(invRef.getInventoryIdReference().toString()));
  }

  @Test 
  void mapPerms() {
    assertEquals(permE, dtoMappers.mapPerms(perm));
  }

  @Test
  void mapUser() {
    assertEquals(userE, dtoMappers.mapUser(userDTO, null));
  }
}
