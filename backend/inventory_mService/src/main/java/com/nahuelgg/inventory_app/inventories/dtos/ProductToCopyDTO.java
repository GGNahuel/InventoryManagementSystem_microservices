package com.nahuelgg.inventory_app.inventories.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder @AllArgsConstructor
public class ProductToCopyDTO {
  private String refId;
  private Integer stock;
}
