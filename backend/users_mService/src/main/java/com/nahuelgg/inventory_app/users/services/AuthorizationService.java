package com.nahuelgg.inventory_app.users.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;

@Service
public class AuthorizationService {
  public boolean checkUserIsAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ContextAuthenticationPrincipal)) return false;
    
    ContextAuthenticationPrincipal auth = (ContextAuthenticationPrincipal) authentication.getPrincipal();
    if (auth.getUser() == null) return false;

    return auth.getUser().isAdmin();
  }
}
