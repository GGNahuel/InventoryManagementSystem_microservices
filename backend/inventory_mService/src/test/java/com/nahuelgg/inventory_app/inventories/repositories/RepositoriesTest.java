package com.nahuelgg.inventory_app.inventories.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ActiveProfiles("test")
public class RepositoriesTest {
  private final InventoryRepository inventoryRepository;
  private final ProductInInvRepository productInInvRepository;

  @Autowired
  public RepositoriesTest(InventoryRepository iRepository, ProductInInvRepository pRepository) {
    this.inventoryRepository = iRepository;
    this.productInInvRepository = pRepository;
  }

  UUID accId1 = UUID.randomUUID(), accId2 = UUID.randomUUID(), 
    pRefId1 = UUID.randomUUID(), pRefId2 = UUID.randomUUID(),
    userRef1 = UUID.randomUUID(), userRef2 = UUID.randomUUID();
  InventoryEntity inv1, inv2, inv3;
  ProductInInvEntity p1, p2, p3, p4;

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

    inv1.setProducts(new ArrayList<>(List.of(p1, p2)));
    inv2.setProducts(new ArrayList<>(List.of(p3)));
    inv3.setProducts(new ArrayList<>(List.of(p4)));

    inventoryRepository.saveAll(List.of(inv1, inv2, inv3));
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
  void existsByNameAndAccountId_returnsExpected() {
    assertTrue(inventoryRepository.existsByNameAndAccountId("inventory_A", accId1));
    assertFalse(inventoryRepository.existsByNameAndAccountId("inventory_B", accId2));
  }

  @Test
  void productInInvRepository_findByReferenceIdAndInventoryId() {
    assertEquals(Optional.of(p1), productInInvRepository.findByReferenceIdAndInventoryId(pRefId1, inv1.getId()));
    assertEquals(Optional.of(p4), productInInvRepository.findByReferenceIdAndInventoryId(pRefId1, inv3.getId()));
    assertEquals(Optional.empty(), productInInvRepository.findByReferenceIdAndInventoryId(pRefId2, inv2.getId()));
  }

  @Test
  void productInInvRepository_findByReferenceAndNotRepeatedInOtherInvs() {   
    assertIterableEquals(List.of(p2.getReferenceId()), productInInvRepository.findReferenceIdsExclusiveToInventory(inv1.getId(), accId1));
    assertIterableEquals(List.of(), productInInvRepository.findReferenceIdsExclusiveToInventory(inv2.getId(), accId1));
  }
}
