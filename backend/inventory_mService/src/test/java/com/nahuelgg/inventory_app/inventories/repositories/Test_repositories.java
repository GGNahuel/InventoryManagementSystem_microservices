package com.nahuelgg.inventory_app.inventories.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class Test_repositories {
  private final InventoryRepository inventoryRepository;
  private final ProductInInvRepository productInInvRepository;
  private final UserReferenceRepository userReferenceRepository;

  @Autowired
  public Test_repositories(InventoryRepository iRepository, ProductInInvRepository pRepository, UserReferenceRepository uRepository) {
    this.inventoryRepository = iRepository;
    this.productInInvRepository = pRepository;
    this.userReferenceRepository = uRepository;
  }

  UUID accId1 = UUID.randomUUID(), accId2 = UUID.randomUUID(), 
    pRefId1 = UUID.randomUUID(), pRefId2 = UUID.randomUUID(),
    userRef1 = UUID.randomUUID(), userRef2 = UUID.randomUUID();
  InventoryEntity inv1, inv2, inv3;
  ProductInInvEntity p1, p2, p3, p4;
  UserReferenceEntity user1, user2;

  @BeforeEach
  void setUp() {
    inv1 = inventoryRepository.save(InventoryEntity.builder()
      .name("inventory_A")
      .accountId(accId1)
    .build());
    inv2 = inventoryRepository.save(InventoryEntity.builder()
      .name("inventory_B")
      .accountId(accId1)
    .build());
    inv3 = inventoryRepository.save(InventoryEntity.builder()
      .name("inventory_A")
      .accountId(accId2)
    .build());

    p1 = productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(pRefId1)
      .stock(4)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    p2 = productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(pRefId2)
      .stock(6)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    p3 = productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(pRefId1)
      .stock(16)
      .inventory(inv2)
    .build());
    p4 = productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(pRefId1)
      .stock(8)
      .inventory(inv3)
    .build());

    user1 = userReferenceRepository.save(UserReferenceEntity.builder()
      .referenceId(userRef1)
    .build());
    user2 = userReferenceRepository.save(UserReferenceEntity.builder()
      .referenceId(userRef2)
    .build());

    inv1.setProducts(new ArrayList<>(List.of(p1, p2)));
    inv1.setUsers(new ArrayList<>(List.of(user1, user2)));

    inv2.setProducts(new ArrayList<>(List.of(p3)));
    inv2.setUsers(new ArrayList<>(List.of(user2)));

    inv3.setProducts(new ArrayList<>(List.of(p4)));
    inv3.setUsers(new ArrayList<>(List.of()));

    inventoryRepository.saveAll(List.of(inv1, inv2, inv3));
  }

  @Test
  void invRepository_findByNameAndAccount() {
    assertEquals(Optional.of(inv1), inventoryRepository.findByNameAndAccountId("inventory_A", accId1));
    assertEquals(Optional.of(inv3), inventoryRepository.findByNameAndAccountId("inventory_A", accId2));
    assertEquals(Optional.empty(), inventoryRepository.findByNameAndAccountId("inventory_B", accId2));
  }

  @Test
  void invRepository_findByAccountId() {
    assertIterableEquals(List.of(inv1, inv2), inventoryRepository.findByAccountId(accId1));
    assertIterableEquals(List.of(), inventoryRepository.findByAccountId(pRefId1));
  }

  @Test
  void invRepository_searchByProductRefId() {
    assertIterableEquals(List.of(inv1, inv2, inv3), inventoryRepository.searchByProductRefId(List.of(pRefId1)));
    assertIterableEquals(List.of(inv1), inventoryRepository.searchByProductRefId(List.of(pRefId2)));
  }

  @Test
  void productInInvRepository_findByReferenceIdAndInventoryId() {
    assertEquals(Optional.of(p1), productInInvRepository.findByReferenceIdAndInventoryId(pRefId1, inv1.getId()));
    assertEquals(Optional.of(p4), productInInvRepository.findByReferenceIdAndInventoryId(pRefId1, inv3.getId()));
    assertEquals(Optional.empty(), productInInvRepository.findByReferenceIdAndInventoryId(pRefId2, inv2.getId()));
  }

  @Test
  void productInInvRepository_findByReferenceAndNotRepeatedInOtherInvs() {   
    assertIterableEquals(List.of(p2.getReferenceId()), productInInvRepository.findReferenceIdsExclusiveToInventory(inv1.getId()));
    assertIterableEquals(List.of(), productInInvRepository.findReferenceIdsExclusiveToInventory(inv2.getId()));
  }

  @Test
  void userRefRepository_findByReferenceId() {
    assertEquals(Optional.of(user1), userReferenceRepository.findByReferenceId(userRef1));
    assertNotEquals(Optional.of(user1), userReferenceRepository.findByReferenceId(userRef2));
  }
}
