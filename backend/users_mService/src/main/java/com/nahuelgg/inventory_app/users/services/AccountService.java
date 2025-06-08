package com.nahuelgg.inventory_app.users.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;

public interface AccountService {
  List<AccountDTO> getAll();
  AccountDTO getById(UUID id);
  AccountDTO create(String username, String password, String passwordRepeated, String adminPassword, String adminPasswordRepeated);
  UserDTO addUser(UserDTO user, UUID accountId, String passwordForNewUser, String passwordRepeated);
  AccountDTO assignInventory(UUID accountId, UUID inventoryId);
  void removeInventoryAssigned(UUID accountId, UUID inventoryId);
  void delete(UUID id);
}
