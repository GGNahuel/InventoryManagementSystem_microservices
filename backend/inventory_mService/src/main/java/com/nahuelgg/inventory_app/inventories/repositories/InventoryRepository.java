package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {
  List<InventoryEntity> findByAccountId(UUID accountId);
}
