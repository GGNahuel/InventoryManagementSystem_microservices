package com.nahuelgg.inventory_app.users.utilities;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ContextAuthenticationPrincipal implements UserDetails {
  private AccountSigned account;
  private UserSigned user;

  @AllArgsConstructor @Data
  public static class AccountSigned {
    private String username;
    private String password;
  }

  @AllArgsConstructor @Data
  public static class UserSigned {
    private String name;
    private String role;
    private boolean isAdmin;
    private List<PermissionsForInventoryDTO> perms;
  }

  /* @AllArgsConstructor @Data // esto en caso de que se quiera usarlos como GranthedAuthorities, habría que formatear los strings
  public static class AuthenticationPermissions {
    private String inventoryReferenceId;
    private List<Permissions> perms;
  } */

  public ContextAuthenticationPrincipal(String username, String password) {
    this.account = new AccountSigned(username, password);
  }

  @Override
  public String getUsername() {
    return this.getAccount().getUsername();
  }

  @Override
  public String getPassword() {
    return this.getAccount().getPassword();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }
}
