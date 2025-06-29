package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {
  List<InventoryEntity> findByAccountId(UUID accountId);

  @Query("select i from InventoryEntity i join i.products p where p.referenceId in ?1")
  List<InventoryEntity> searchByProductRefId(List<UUID> referenceIds);
  /*group by i having count(distinct p.referenceId) = ?2 */

  boolean existsByNameAndAccountId(String name, UUID accountId);
}
