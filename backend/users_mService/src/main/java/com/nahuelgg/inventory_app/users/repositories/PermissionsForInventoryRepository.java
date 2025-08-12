package com.nahuelgg.inventory_app.users.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;


@Repository
public interface PermissionsForInventoryRepository extends JpaRepository<PermissionsForInventoryEntity, UUID>{
  @Query("select p from permission_for_inventory p where p.inventoryReference.inventoryIdReference = ?1")
  List<PermissionsForInventoryEntity> findByReferencedInventoryId(UUID idReferenced);

  // la query ya incluye el futuro cambio de inventory reference a ElementCollection
  @Query(
    value =  "select * from permission_for_inventory as p where p.inventory_reference_id = ?1 and p.associated_user_id = ?2", 
    nativeQuery = true
  )
  Optional<PermissionsForInventoryEntity> findByInventoryReferenceIdAndUserId(UUID inventoryRefId, UUID userId);
}
