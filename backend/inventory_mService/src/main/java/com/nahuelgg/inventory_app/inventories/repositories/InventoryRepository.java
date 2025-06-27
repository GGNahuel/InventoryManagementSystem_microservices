package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {
  @Query("select i from InventoryEntity i where i.name = ?1 and i.accountId = ?2")
  Optional<InventoryEntity> findByNameAndAccountId(String name, UUID accountId);

  List<InventoryEntity> findByAccountId(UUID accountId);

  @Query("select i from InventoryEntity i join i.products p where p.referenceId in ?1")
  List<InventoryEntity> searchByProductRefId(List<UUID> referenceIds);
  /*group by i having count(distinct p.referenceId) = ?2 */
}
