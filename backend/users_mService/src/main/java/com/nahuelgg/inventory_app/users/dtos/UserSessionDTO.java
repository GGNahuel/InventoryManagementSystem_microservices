package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder
@AllArgsConstructor
public class UserSessionDTO {
  private String id;
  private String name;
  private String role;
  private Boolean isAdmin;
  private List<PermissionsForInventoryDTO> inventoryPerms;
}
