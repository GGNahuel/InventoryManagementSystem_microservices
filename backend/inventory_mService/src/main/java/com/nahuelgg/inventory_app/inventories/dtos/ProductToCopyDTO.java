package com.nahuelgg.inventory_app.inventories.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ProductToCopyDTO {
  private String refId;
  private Integer stock;
}
