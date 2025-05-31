package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import java.util.List;

@Repository
public interface ProductInInvRepository extends JpaRepository<ProductInInvEntity, UUID> {
  @Query("select p from ProductInInvEntity p where p.isAvailable = ?1 and p.inventory.id = ?2")
  List<ProductInInvEntity> findByIsAvailableAndInvId(Boolean isAvailable, UUID invId);

  List<ProductInInvEntity> findByReferenceId(UUID referenceId);
}
