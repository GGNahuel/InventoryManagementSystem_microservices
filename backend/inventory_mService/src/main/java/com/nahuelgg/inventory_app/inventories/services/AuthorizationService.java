package com.nahuelgg.inventory_app.inventories.services;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal.PermsForInv;

@Service
public class AuthorizationService {
  public boolean checkAccountIsLogged() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;

    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    return auth.getAccount().getUsername() != null;
  }

  public boolean checkUserIsAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    if (auth.getUser() == null) return false;

    return auth.getUser().isAdmin();
  }

  public boolean checkUserHasPerm(Permissions perm, String invId) {
    List<Permissions> perms = List.of(Permissions.values());
    if (!perms.contains(perm)) throw new RuntimeException("Se ha ingresado un permiso inexistente al método: " + perm);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;

    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    if (auth.getUser() == null) return false;
    if (auth.getUser().isAdmin()) return true;
    if (auth.getUser().getPerms() == null) return false;

    PermsForInv permObject = auth.getUser().getPerms().stream().filter(permDto -> permDto.getInventoryReferenceId().equals(invId)).findFirst().orElse(null);
    return permObject != null && permObject.getPerms().contains(perm);
  }

  public boolean checkActionIsToLoggedAccount(String accountId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    AccountSigned accountSigned = auth.getAccount();
    if (accountSigned == null || accountSigned.getUsername() == null) return false;

    return accountSigned.getId().equals(accountId);
  }

  public boolean checkAccountIdAndUserPerm(String accountId, Permissions perm, String invId) {
    return checkUserHasPerm(perm, invId) && checkActionIsToLoggedAccount(accountId);
  }
}
