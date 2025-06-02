package com.nahuelgg.inventory_app.users.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;


@Repository
public interface PermissionsForInventoryRepository extends JpaRepository<PermissionsForInventoryEntity, UUID>{
  @Query("select p from permissions_for_inventory p where p.inventoryReference.inventoryIdReference = ?1")
  List<PermissionsForInventoryEntity> findByReferencedInventoryId(UUID idReferenced);
}
