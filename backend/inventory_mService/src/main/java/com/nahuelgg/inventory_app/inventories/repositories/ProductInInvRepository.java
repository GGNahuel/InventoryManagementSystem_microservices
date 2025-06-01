package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;

@Repository
public interface ProductInInvRepository extends JpaRepository<ProductInInvEntity, UUID> {
  @Query("select p from ProductInInvEntity p where p.isAvailable = ?1 and p.inventory.id = ?2")
  List<ProductInInvEntity> findByIsAvailableAndInvId(Boolean isAvailable, UUID invId);

  @Query("select p from ProductInInvEntity p where p.referenceId = ?1 and  p.inventory.id = ?2")
  Optional<ProductInInvEntity> findByReferenceIdAndInventoryId(UUID referenceId, UUID inventoryId);

  @Query("select p from ProductInInvEntity p where p.referenceId in ?1 and p.inventory.id not in ?2")
  List<ProductInInvEntity> findThoseWichAreNotInOthersInvs(List<UUID> referenceIds, List<UUID> inventoryIds);
}
