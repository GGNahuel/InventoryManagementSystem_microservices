package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.enums.Permissions;

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

  @Data @Builder @AllArgsConstructor
  public static class InventoryPermsDTO {
    private List<Permissions> permissions;
    private String idOfInventoryReferenced;
  }
}
