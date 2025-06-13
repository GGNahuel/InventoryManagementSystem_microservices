package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserFromUsersMSDTO {
  private String id;
  private String name;
  private String role;
  private List<InventoryPermsDTO> inventoryPerms;

  @Data @AllArgsConstructor
  public static class InventoryPermsDTO {
    private String id;
    private List<String> permissions;
    private String idOfInventoryReferenced;
  }
}
