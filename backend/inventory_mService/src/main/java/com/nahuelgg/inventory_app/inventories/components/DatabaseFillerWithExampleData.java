package com.nahuelgg.inventory_app.inventories.components;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnExpression("!'${spring.profiles.active:}'.contains('test')")
@RequiredArgsConstructor
public class DatabaseFillerWithExampleData implements CommandLineRunner {
  private final InventoryRepository inventoryRepository;
  private final ProductInInvRepository productInInvRepository;
  
  @Override
  public void run(String... args) throws Exception {
    final UUID accountId = UUID.fromString("12341234-0000-0000-0000-11223344acc1");
    final UUID inventoryRefId1 = UUID.fromString("12341234-0000-1000-0001-100010001000");
    final UUID inventoryRefId2 = UUID.fromString("12341234-0000-1000-0002-100010001000");
    final String productRefBaseId = "00000000-0000-0000-0000-0000000000";

    if (!inventoryRepository.findByAccountId(accountId).isEmpty()) return;

    InventoryEntity inv1 = inventoryRepository.save(InventoryEntity.builder()
      .id(inventoryRefId1)
      .name("Sucursal norte")
      .accountId(accountId)
    .build());

    InventoryEntity inv2 = inventoryRepository.save(InventoryEntity.builder()
      .id(inventoryRefId2)
      .name("Sucursal centro")
      .accountId(accountId)
    .build());

    // Productos en inventario 1
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "01"))
      .stock(38)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "02"))
      .stock(25)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "03"))
      .stock(20)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "04"))
      .stock(16)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "05"))
      .stock(18)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "06"))
      .stock(22)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "07"))
      .stock(22)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "08"))
      .stock(15)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "09"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "10"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "11"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "12"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "13"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "14"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "15"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "16"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv1)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "17"))
      .stock(2)
      .isAvailable(true)
      .inventory(inv1)
    .build());

    // Productos en inventario 2
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "01"))
      .stock(38)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "02"))
      .stock(25)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "03"))
      .stock(20)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "04"))
      .stock(16)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "05"))
      .stock(18)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "06"))
      .stock(22)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "18"))
      .stock(22)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "08"))
      .stock(15)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "09"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "10"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "11"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "12"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "19"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "14"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "15"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "16"))
      .stock(12)
      .isAvailable(true)
      .inventory(inv2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productRefBaseId + "20"))
      .stock(16)
      .isAvailable(true)
      .inventory(inv2)
    .build());
  }
}
