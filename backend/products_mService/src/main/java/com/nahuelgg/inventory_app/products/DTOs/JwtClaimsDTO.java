package com.nahuelgg.inventory_app.products.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.products.enums.Permissions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class JwtClaimsDTO {
  private String accountId;
  private String userName;
  private String userRole;
  private boolean isAdmin;
  private List<PermissionsForInventoryDTO> userPerms;

  @Data @Builder @AllArgsConstructor @NoArgsConstructor
  public static class PermissionsForInventoryDTO {
    private String id;
    private List<Permissions> permissions;
    private String idOfInventoryReferenced;
  }
}
