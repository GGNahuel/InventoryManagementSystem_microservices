package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserRegistrationDTO {
  private String name;
  private String role;
  private String password;
  private String passwordRepeated;
  private List<PermissionsForInventoryDTO> inventoryPerms;
}
