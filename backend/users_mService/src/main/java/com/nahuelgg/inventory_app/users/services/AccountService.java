package com.nahuelgg.inventory_app.users.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.AccountRegistrationDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.dtos.UserRegistrationDTO;

public interface AccountService {
  List<AccountDTO> getAll();
  AccountDTO getById(UUID id);
  AccountDTO create(AccountRegistrationDTO info);
  UserDTO addUser(UUID accountId, UserRegistrationDTO info);
  void assignInventory(UUID accountId, UUID inventoryId);
  void removeInventoryAssigned(UUID accountId, UUID inventoryId);
  void delete(UUID id);
}
