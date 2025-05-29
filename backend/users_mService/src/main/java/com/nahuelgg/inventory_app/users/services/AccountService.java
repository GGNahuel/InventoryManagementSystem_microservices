package com.nahuelgg.inventory_app.users.services;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.UserEntity;

public interface AccountService extends UserDetailsService {
  AccountDTO getById(UUID id);
  AccountDTO create(String username, String password, String passwordRepeated, String adminPassword, String adminPasswordRepeated);
  UserEntity addUser(UserDTO user, UUID accountId);
  void assignInventory(UUID accountId, String inventoryId);
  void removeInventoryAssigned(UUID accountId, String inventoryId);
  // este método también tendría que eliminar todos los inventarios asociados y a su vez los productos asociados a estos,
  // en cada micro servicio según corresponda
  void delete(UUID id);
}
