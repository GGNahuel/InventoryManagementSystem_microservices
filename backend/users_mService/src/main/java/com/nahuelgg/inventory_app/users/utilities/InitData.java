package com.nahuelgg.inventory_app.users.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("test")
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final InventoryRefRepository inventoryRefRepository;
  private final PermissionsForInventoryRepository pForInventoryRepository;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    InventoryRefEntity i = inventoryRefRepository.save(InventoryRefEntity.builder().inventoryIdReference(UUID.randomUUID()).build());

    AccountEntity a1 = accountRepository.save(AccountEntity.builder()
      .username("account")
      .password("456")
      .inventoriesReferences(new ArrayList<>(List.of(i)))
    .build());

    // estas se supone que apuntarían a referencias de inventario distintas
    PermissionsForInventoryEntity p1 = pForInventoryRepository.save(PermissionsForInventoryEntity.builder().inventoryReference(i).permissions("a,b,c").build());
    PermissionsForInventoryEntity p2 = pForInventoryRepository.save(PermissionsForInventoryEntity.builder().inventoryReference(i).permissions("a,c").build());

    UserEntity u1 = userRepository.save(UserEntity.builder()
      .name("admin").password("123")
      .isAdmin(true).role("admin")
      .associatedAccount(a1)
    .build());
    UserEntity u2 = userRepository.save(UserEntity.builder()
      .name("general").password("123")
      .isAdmin(false).role("general")
      .inventoryPerms(new ArrayList<>(List.of(p1, p2)))
      .associatedAccount(a1)
    .build());

    a1.setUsers(new ArrayList<>(List.of(u1, u2)));
    accountRepository.save(a1);
  }
}
