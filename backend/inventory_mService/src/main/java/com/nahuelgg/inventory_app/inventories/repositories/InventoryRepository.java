package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {
  @Query("select i from InventoryEntity i where i.name = ?1 and i.accountId = ?2")
  List<InventoryEntity> findByNameAndAccountId(String name, UUID accountId);

  List<InventoryEntity> findByAccountId(UUID accountId);
}
