package com.nahuelgg.inventory_app.products.utilities;

import java.util.UUID;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;

public class Mappers {
  public ProductDTO mapEntityToDTO(ProductEntity p) {
    return ProductDTO.builder()
      .id(p.getId() != null ? p.getId().toString() : null)
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories())
      .accountId(p.getAccountId().toString())
    .build();
  }

  public ProductEntity mapDTOToEntity(ProductDTO p) {
    return ProductEntity.builder()
      .id(p.getId() != null ? UUID.fromString(p.getId()) : null)
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories())
      .accountId(UUID.fromString(p.getAccountId()))
    .build();
  }
}
