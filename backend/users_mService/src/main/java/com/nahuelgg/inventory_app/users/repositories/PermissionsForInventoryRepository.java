package com.nahuelgg.inventory_app.users.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import java.util.List;


@Repository
public interface PermissionsForInventoryRepository extends JpaRepository<PermissionsForInventoryEntity, UUID>{
  @Query("select p from PermissionsForInventoryEntity p where associatedUserId.id = ?0")
  List<PermissionsForInventoryEntity> findByAssociatedUserId(UUID associatedUserId);
}
