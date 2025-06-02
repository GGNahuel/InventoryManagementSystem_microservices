package com.nahuelgg.inventory_app.users.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;

@Repository
public interface InventoryRefRepository extends JpaRepository<InventoryRefEntity, UUID>{
  Optional<InventoryRefEntity> findByInventoryIdReference(UUID inventoryIdReference);
}
