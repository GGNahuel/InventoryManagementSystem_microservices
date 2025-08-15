package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.enums.Permissions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class PermissionsForInventoryDTO {
  private List<Permissions> permissions;
  private String idOfInventoryReferenced;
}
