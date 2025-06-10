package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class JwtClaimsDTO {
  private String accountUsername;
  private String userName;
  private String userRole;
  private boolean isAdmin;
  private List<PermissionsForInventoryDTO> userPerms;
}
