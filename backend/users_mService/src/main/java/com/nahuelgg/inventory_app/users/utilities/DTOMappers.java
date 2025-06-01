package com.nahuelgg.inventory_app.users.utilities;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;

@Component
public class DTOMappers {
  private final InventoryRefRepository inventoryRefRepository;

  public DTOMappers(InventoryRefRepository inventoryRefRepository) {
    this.inventoryRefRepository = inventoryRefRepository;
  }

  public InventoryRefEntity mapInventoryRef(String inventoryIdRef) {
    return inventoryRefRepository.findByInventoryIdReference(inventoryIdRef).orElseThrow(
      () -> new RuntimeException("")
    );
  }

  public PermissionsForInventoryEntity mapPerms(PermissionsForInventoryDTO dto) {
    String permissionsString = "";
    for (int i = 0; i < dto.getPermissions().size(); i++) {
      String perm = dto.getPermissions().get(i).toString();
      permissionsString.concat(i < dto.getPermissions().size() -1 ? perm + "," : perm);
    }

    return PermissionsForInventoryEntity.builder()
      .id(UUID.fromString(dto.getId()))
      .permissions(permissionsString)
      .inventoryReference(mapInventoryRef(dto.getIdOfInventoryReferenced()))
    .build();
  }

  public UserEntity mapUser(UserDTO dto, AccountEntity account) {
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
