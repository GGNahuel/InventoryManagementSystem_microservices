package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.users.enums.Permissions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class PermissionsForInventoryDTO {
  private String id;
  private List<Permissions> permissions;
  private String idOfInventoryReference;
}
