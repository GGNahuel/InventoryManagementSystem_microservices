package com.nahuelgg.inventory_app.products.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class CategoryDTO {
  private String id;
  private String name;
}
