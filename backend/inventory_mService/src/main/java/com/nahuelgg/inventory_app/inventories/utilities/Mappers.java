package com.nahuelgg.inventory_app.inventories.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;

public class Mappers {
  public ProductInInvDTO mapProductsFromMSToDTO(ProductFromProductsMSDTO p, ProductInInvEntity pEntity) {
    return ProductInInvDTO.builder()
      .refId(p.getId())
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories())
      .stock(pEntity.getStock())
      .isAvailable(pEntity.getIsAvailable())
    .build();
  }

  public ProductFromProductsMSDTO mapProductInput(ProductInputDTO p) {
    return ProductFromProductsMSDTO.builder()
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories())
    .build();
  }

  public InventoryDTO mapInvEntity(InventoryEntity inv, List<ProductFromProductsMSDTO> products) {
    List<ProductInInvDTO> productsMapped = new ArrayList<>();

    for (int i = 0; i < products.size(); i++) {
      ProductFromProductsMSDTO productReference = products.get(i);
      ProductInInvEntity productInInvEntity = inv.getProducts().stream().filter(
        p -> p.getReferenceId().equals(UUID.fromString(productReference.getId()))
      ).findFirst().orElse(null);

      if (productInInvEntity != null) productsMapped.add(mapProductsFromMSToDTO(productReference, productInInvEntity));
    }

    return InventoryDTO.builder()
      .id(inv.getId().toString())
      .name(inv.getName())
      .accountId(inv.getAccountId().toString())
      .usersIds(inv.getUserReferences().stream().map(
        userRefModel -> userRefModel.getReferenceId().toString()
      ).toList())
      .products(productsMapped)
    .build();
  }
}
