package com.nahuelgg.inventory_app.inventories.dtos.schemaInputs;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class EditProductInputDTO {
  private String refId;
  private String name;
  private String brand;
  private String model;
  private String description;
  private Integer unitPrice;
  private List<String> categories;

  public ProductFromProductsMSDTO mapToProductFromProductService (String accountId) {
    return ProductFromProductsMSDTO.builder()
      .name(this.name).brand(this.brand).model(this.model)
      .description(this.description).categories(this.categories)
      .unitPrice(this.unitPrice)
      .accountId(accountId)
    .build();
  }
}
