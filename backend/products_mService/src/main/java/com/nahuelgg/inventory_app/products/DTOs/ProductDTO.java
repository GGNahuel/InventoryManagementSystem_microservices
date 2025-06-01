package com.nahuelgg.inventory_app.products.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductDTO {
  private String id;
  private String name;
  private String brand;
  private String model;
  private String description;
  private Double unitPrice;
  private String accountId;
  private List<String> categories;
}
