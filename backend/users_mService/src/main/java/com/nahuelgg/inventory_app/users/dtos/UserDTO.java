package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class UserDTO {
  private String id;
  private String name;
  private String role;
  private List<PermissionsForInventoryDTO> inventoryPerms;
}
