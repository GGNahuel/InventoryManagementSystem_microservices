package com.nahuelgg.inventory_app.inventories.utilities;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.dtos.PermissionsForInventoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ContextAuthenticationPrincipal {
  private AccountSigned account;
  private UserSigned user;

  @AllArgsConstructor @Data
  public static class AccountSigned {
    private String username;
    private String id;
  }

  @AllArgsConstructor @Data
  public static class UserSigned {
    private String name;
    private String role;
    private boolean isAdmin;
    private List<PermissionsForInventoryDTO> perms;
  }
}
