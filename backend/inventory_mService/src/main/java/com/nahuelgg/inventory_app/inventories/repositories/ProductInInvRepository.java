package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;

@Repository
public interface ProductInInvRepository extends JpaRepository<ProductInInvEntity, UUID> {
  @Query("select p from product_in_inv p join p.inventory i where p.referenceId = ?1 and i.id = ?2")
  Optional<ProductInInvEntity> findByReferenceIdAndInventoryId(UUID referenceId, UUID inventoryId);

  List<ProductInInvEntity> findByInventory(InventoryEntity inventory);

  List<ProductInInvEntity> findByReferenceId(UUID referenceId);

  // esta query buscará las ref ids de los productos que estén solamente en el inventario enviado
  // Primero selecciona las entidades que estén asociadas a un solo inventario, luego se fijará si esas entidades seleccionadas se encuentran
  @Query("""
    select p.referenceId from product_in_inv p where 
    p.inventory.id = ?1 and
    p.referenceId in (
      select p2.referenceId from product_in_inv p2 where
      p.inventory.accountId = ?2
      group by p2.referenceId
      having count(p2.referenceId) = 1
    )
  """)
  List<UUID> findReferenceIdsExclusiveToInventory(UUID idOfParentInventory, UUID accountId);
}
