package com.nahuelgg.inventory_app.users.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ActiveProfiles("test")
public class RepositoriesTest {
  private final AccountRepository accRepo;
  private final UserRepository userRepo;
  private final InventoryRefRepository invRefRepo;
  private final PermissionsForInventoryRepository permsRepo;

  @Autowired
  public RepositoriesTest(AccountRepository accRepo, UserRepository userRepo, InventoryRefRepository invRepo, PermissionsForInventoryRepository permRepo) {
    this.accRepo = accRepo;
    this.userRepo = userRepo;
    this.invRefRepo = invRepo;
    this.permsRepo = permRepo;
  }

  private UUID invRefId = UUID.randomUUID();
  private InventoryRefEntity invRef;
  private AccountEntity acc1;
  // private UserEntity user1, user2;
  private PermissionsForInventoryEntity perms1, perms2;

  @BeforeEach
  void beforeEach() {
    InventoryRefEntity i = invRefRepo.save(InventoryRefEntity.builder().inventoryIdReference(invRefId).build());

    AccountEntity a1 = accRepo.save(AccountEntity.builder()
      .username("account")
      .password("456")
      .inventoriesReferences(new ArrayList<>(List.of(i)))
    .build());

    // estas se supone que apuntarían a referencias de inventario distintas
    PermissionsForInventoryEntity p1 = permsRepo.save(PermissionsForInventoryEntity.builder().inventoryReference(i).permissions("a,b,c").build());
    PermissionsForInventoryEntity p2 = permsRepo.save(PermissionsForInventoryEntity.builder().inventoryReference(i).permissions("a,c").build());

    UserEntity u1 = userRepo.save(UserEntity.builder()
      .name("admin").password("123")
      .isAdmin(true).role("admin")
      .associatedAccount(a1)
    .build());
    UserEntity u2 = userRepo.save(UserEntity.builder()
      .name("general").password("123")
      .isAdmin(false).role("general")
      .inventoryPerms(new ArrayList<>(List.of(p1, p2)))
      .associatedAccount(a1)
    .build());

    a1.setUsers(new ArrayList<>(List.of(u1, u2)));
    accRepo.save(a1);

    invRef = i;
    perms1 = p1;
    perms2 = p2;
    acc1 = a1;
  }

  @Test
  void accountRepository_findByUserName() {
    assertEquals(Optional.of(acc1), accRepo.findByUsername("account"));
    assertEquals(Optional.empty(), accRepo.findByUsername("notFound"));
  }

  @Test
  void inventoryRefRepository() {
    assertEquals(Optional.of(invRef), invRefRepo.findByInventoryIdReference(invRefId));
    assertEquals(Optional.empty(), invRefRepo.findByInventoryIdReference(UUID.randomUUID()));
  }

  @Test
  void permsInInvRepository() {
    assertEquals(List.of(perms1, perms2), permsRepo.findByReferencedInventoryId(invRefId));
    assertEquals(List.of(), permsRepo.findByReferencedInventoryId(UUID.randomUUID()));
  }
}
