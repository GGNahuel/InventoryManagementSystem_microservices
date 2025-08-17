package com.nahuelgg.inventory_app.users.components;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.enums.Permissions;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DTOMappers {
  private final InventoryRefRepository inventoryRefRepository;
  private final AccountRepository accountRepository;

  public InventoryRefEntity mapInventoryRef(String inventoryIdRef) {
    return inventoryRefRepository.findByInventoryIdReference(UUID.fromString(inventoryIdRef)).orElseThrow(
      () -> new ResourceNotFoundException("referencia de inventario", "id de referencia", inventoryIdRef)
    );
  }

  public String mapSpecificPermissions(List<Permissions> perms) {
    String permissionsString = "";
    for (int i = 0; i < perms.size(); i++) {
      String perm = perms.get(i).toString();
      permissionsString += i < perms.size() -1 ? perm + "," : perm;
    }

    return permissionsString;
  }

  public PermissionsForInventoryEntity mapPerms(PermissionsForInventoryDTO dto) {
    String permissionsString = mapSpecificPermissions(dto.getPermissions());

    return PermissionsForInventoryEntity.builder()
      .permissions(permissionsString)
      .inventoryReference(mapInventoryRef(dto.getIdOfInventoryReferenced()))
    .build();
  }

  public UserEntity mapUser(UserDTO dto, UUID accountId) {
    AccountEntity account = accountRepository.findById(accountId).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "id", accountId.toString())
    );

    return UserEntity.builder()
      .id(UUID.fromString(dto.getId()))
      .name(dto.getName())
      .role(dto.getRole())
      .inventoryPerms(dto.getInventoryPerms() != null ? dto.getInventoryPerms().stream().map(
        permsDto -> mapPerms(permsDto)
      ).toList() : null)
      .associatedAccount(account)
    .build();
  }
}
