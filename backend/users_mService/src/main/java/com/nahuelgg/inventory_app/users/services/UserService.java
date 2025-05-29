package com.nahuelgg.inventory_app.users.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;

public interface UserService {
  UserDTO getById(UUID id);
  UserDTO edit(UserDTO updatedUser);
  UserDTO assignNewPerms(List<PermissionsForInventoryDTO> permissions);
  void delete(UUID id);
}
