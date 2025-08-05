package com.nahuelgg.inventory_app.products.services;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.nahuelgg.inventory_app.products.enums.Permissions;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal.PermsForInv;

@Service
public class AuthorizationService {
  public boolean checkUserIsAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    if (auth.getUser() == null) return false;

    return auth.getUser().isAdmin();
  }

  public boolean checkUserHasPerm(String perm, String invId) {
    List<String> perms = List.of(Permissions.values()).stream().map(p -> p.toString()).toList();
    if (!perms.contains(perm)) throw new RuntimeException("Se ha ingresado un permiso inexistente al método: " + perm);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    if (auth.getUser() == null) return false;
    if (auth.getUser().isAdmin()) return true;
    if (auth.getUser().getPerms() == null) return false;
    
    if (
      (
        perm.equals(Permissions.editProductReferences.toString()) && 
        auth.getUser().getPerms().stream().anyMatch(permDto -> permDto.getPerms().contains(Permissions.editProductReferences))
      ) || (
        perm.equals(Permissions.deleteProductReferences.toString()) && 
        auth.getUser().getPerms().stream().anyMatch(permDto -> permDto.getPerms().contains(Permissions.deleteProductReferences))
      )
    ) return true;

    PermsForInv permObject = auth.getUser().getPerms().stream().filter(
      permDto -> permDto.getInventoryReferenceId().equals(invId))
    .findFirst().orElse(null);
    return permObject != null && permObject.getPerms().contains(Permissions.valueOf(perm));
  }

  public boolean checkActionIsToLoggedAccount(String accountId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    AccountSigned accountSigned = auth.getAccount();
    if (accountSigned == null || accountSigned.getUsername() == null) return false;

    return accountSigned.getId().equals(accountId);
  }
}
