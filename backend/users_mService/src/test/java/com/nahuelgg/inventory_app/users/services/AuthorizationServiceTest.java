package com.nahuelgg.inventory_app.users.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;

public class AuthorizationServiceTest {
  AuthorizationService service = new AuthorizationService();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void checkUserIsAdmin_success() {
    ContextAuthenticationPrincipal principal = ContextAuthenticationPrincipal.builder()
      .account(new AccountSigned("accUsername", "testPass"))
      .user(new UserSigned("user", "role", true, null))
    .build();

    var auth = new UsernamePasswordAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertTrue(service.checkUserIsAdmin());
  }

  @Test
  void checkUserIs_deniedIfNoAuthentication() {
    SecurityContextHolder.clearContext();
    assertFalse(service.checkUserIsAdmin());
  }

  @Test
  void checkUserIsAdmin_deniedIfPrincipalIsNotContextAuthenticationPrincipal() {
    var auth = new UsernamePasswordAuthenticationToken("string-user", null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertFalse(service.checkUserIsAdmin());
  }

  @Test
  void checkUserIsAdmin_DeniedIfUserIsNull() {
    ContextAuthenticationPrincipal principal = ContextAuthenticationPrincipal.builder()
      .user(null)
    .build();

    var auth = new UsernamePasswordAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertFalse(service.checkUserIsAdmin());
  }
}
