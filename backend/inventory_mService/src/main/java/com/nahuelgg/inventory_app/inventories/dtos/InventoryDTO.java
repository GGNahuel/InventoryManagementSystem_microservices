package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class InventoryDTO {
  private String id;
  private String name;
  private String accountId;
  private List<String> referenceIdsOfUsers;
  private List<ProductInInvDTO> products;
}
