package com.nahuelgg.inventory_app.users.services;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;

public interface UserService {
  UserDTO getById(UUID id, UUID accountId);
  UserDTO edit(UserDTO updatedUser, UUID accountId);
  UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId, UUID accountId) throws JsonProcessingException;
  void delete(UUID id, UUID accountId);
}
// TODO: ver cómo hacer que los users solo puedan editar productos con ciertas categorías?