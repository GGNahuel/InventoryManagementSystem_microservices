package com.nahuelgg.inventory_app.products.utilities;

import java.util.List;

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
    private List<PermsForInv> perms;
  }

  // este tendría que extender de Granthed authority, ver constructor
  @AllArgsConstructor @Data
  public static class PermsForInv {
    private String inventoryReferenceId;
    private List<String> perms;
  }
}
