package com.nahuelgg.inventory_app.users.services;

import java.util.UUID;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;

public interface UserService {
  UserDTO getById(UUID id);
  UserDTO edit(UserDTO updatedUser);
  UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId);
  void delete(UUID id);
  boolean checkUserIsAdmin();
  void loginAsUser(UUID id, String password);
  void logoutUser();
}
// ver cómo hacer que los users solo puedan editar productos con ciertas categorías?