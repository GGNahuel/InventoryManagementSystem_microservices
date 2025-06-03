package com.nahuelgg.inventory_app.users.utilities;

import java.util.Arrays;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.AccountSessionDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.dtos.UserSessionDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.enums.Permissions;

public class EntityMappers {
  
  public PermissionsForInventoryDTO mapPerms(PermissionsForInventoryEntity e) {
    return PermissionsForInventoryDTO.builder()
      .id(e.getId().toString())
      .permissions(Arrays.asList((e.getPermissions().split(","))).stream().map(
        permString -> Permissions.valueOf(permString)
      ).toList())
      .idOfInventoryReferenced(e.getInventoryReference().getInventoryIdReference().toString())
    .build();
  }

  public UserDTO mapUser(UserEntity e) {
    return UserDTO.builder()
      .id(e.getId().toString())
      .name(e.getName())
      .role(e.getRole())
      .inventoryPerms(e.getInventoryPerms() != null ? 
        e.getInventoryPerms().stream().map(
          permsEntity -> mapPerms(permsEntity)
        ).toList()
      : null)
    .build();
  }

  public AccountDTO mapAccount(AccountEntity e) {
    return AccountDTO.builder()
      .id(e.getId().toString())
      .username(e.getUsername())
      .idsOfInventoryReferred(e.getInventoriesReferences() != null ? 
        e.getInventoriesReferences().stream().map(
          idRefEntity -> idRefEntity.getInventoryIdReference().toString()
        ).toList() 
      : null)
      .users(e.getUsers() != null ? 
        e.getUsers().stream().map(
          userEntity -> mapUser(userEntity)
        ).toList() 
      : null)
    .build();
  }

  public UserSessionDTO mapUser_session(UserEntity u) {
    return UserSessionDTO.builder()
      .id(u.getId().toString())
      .name(u.getName())
      .role(u.getRole())
      .isAdmin(u.getIsAdmin())
      .inventoryPerms(u.getInventoryPerms() != null ? 
        u.getInventoryPerms().stream().map(
          permsEntity -> mapPerms(permsEntity)
        ).toList()
      : null)
    .build();
  }

  public AccountSessionDTO mapAccount_session(AccountEntity e) {
    return AccountSessionDTO.builder()
      .id(e.getId().toString())
      .username(e.getUsername())
      .users(e.getUsers() != null ? 
        e.getUsers().stream().map(
          userEntity -> mapUser_session(userEntity)
        ).toList() 
      : null)
    .build();
  }
}
