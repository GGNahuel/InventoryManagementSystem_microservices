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
  @Query("select p from ProductInInvEntity p join p.inventory i where p.referenceId = ?1 and i.id = ?2")
  Optional<ProductInInvEntity> findByReferenceIdAndInventoryId(UUID referenceId, UUID inventoryId);

  @Query("""
    select p.referenceId from ProductInInvEntity p where 
    p.inventory.id = ?1 and
    p.referenceId in (
      select p2.referenceId from ProductInInvEntity p2
      group by p2.referenceId
      having count(p2.referenceId) = 1
    )
  """)
  List<UUID> findReferenceIdsExclusiveToInventory(UUID idOfInventoryToDelete);
}
