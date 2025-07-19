package com.nahuelgg.inventory_app.users.controllers;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/e2e")
@Profile("e2e")
@RequiredArgsConstructor
public class E2ETestController {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final InventoryRefRepository inventoryRefRepository;
  private final PermissionsForInventoryRepository permRepository;

  @GetMapping("/accounts")
  public List<AccountEntity> getAllAccounts() {
    return accountRepository.findAll();
  }

  @GetMapping("/users")
  public List<UserEntity> getAllUsers() {
    return userRepository.findAll();
  }

  @GetMapping("/inventoryRefs")
  public List<InventoryRefEntity> getAllInventoryRefs() {
    return inventoryRefRepository.findAll();
  }

  @GetMapping("/permissionForInv")
  public List<PermissionsForInventoryEntity> getAllPermsForInv() {
    return permRepository.findAll();
  }
}
