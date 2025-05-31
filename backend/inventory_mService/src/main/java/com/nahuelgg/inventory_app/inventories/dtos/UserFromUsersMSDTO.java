package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserFromUsersMSDTO {
  @Data @AllArgsConstructor
  public class InventoryPermsDTO {
    private String id;
    private List<String> permissions;
    private String idOfInventoryReferenced;
  }

  private String id;
  private String role;
  private List<InventoryPermsDTO> inventoryPerms;
}
