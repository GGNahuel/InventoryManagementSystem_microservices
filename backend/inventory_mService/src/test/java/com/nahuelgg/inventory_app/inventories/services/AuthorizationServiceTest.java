package com.nahuelgg.inventory_app.inventories.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal.PermsForInv;
import com.nahuelgg.inventory_app.inventories.utilities.ContextAuthenticationPrincipal.UserSigned;

public class AuthorizationServiceTest {
  AuthorizationService authorizationService = new AuthorizationService();

  @Test
  void checkAccountIsLogged_workAsExpected() {
    assertFalse(authorizationService.checkAccountIsLogged(), 
      "Debería retornar falso cuando no hay autenticación");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken("user", null));
    assertFalse(authorizationService.checkAccountIsLogged(), 
      "Debería retornar false si el principal de la autenticación no es instancia de la clase esperada");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned(null, null)).build(),
      null
    ));
    assertFalse(authorizationService.checkAccountIsLogged(),
      "Debería retornar falso si la autenticación en el contexto no tiene nombre de usuario");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned("username", null)).build(),
      null
    ));
    assertTrue(authorizationService.checkAccountIsLogged());
  }

  @Test
  void checkUserIsAdmin() {
    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned(null, null)).build(),
      null
    ));
    assertFalse(authorizationService.checkAccountIsLogged(),
      "Debería retornar falso si la autenticación en el contexto no tiene nombre de usuario");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned("account", "accId"))
        .user(new UserSigned("name", "role", false, List.of()))
      .build(),
      null
    ));
    assertFalse(authorizationService.checkUserIsAdmin(), "Debería dar false si el usuario no es admin");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned("account", "accId"))
        .user(new UserSigned("name", "role", true, List.of()))
      .build(),
      null
    ));
    assertTrue(authorizationService.checkUserIsAdmin(), "Debería dar true si el sub-usuario es admin");
  }

  @Test
  void checkUserHasPerm() {
    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned(null, null)).build(),
      null
    ));
    assertFalse(authorizationService.checkAccountIsLogged(),
      "Debería retornar falso si la autenticación en el contexto no tiene nombre de usuario");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned("account", "accId"))
        .user(new UserSigned("name", "role", true, List.of()))
      .build(),
      null
    ));
    assertTrue(authorizationService.checkUserHasPerm(Permissions.editInventory, null),
      "Debería dar true si el sub-usuario es admin");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned("account", "accId"))
        .user(new UserSigned("name", "role", false, List.of(
          new PermsForInv("invId", List.of(Permissions.editInventory, Permissions.addProducts))
        )))
      .build(),
      null
    ));
    assertTrue(authorizationService.checkUserHasPerm(Permissions.addProducts, "invId"),
      "Debería dar true si tiene el permiso indicado para el inventario indicado");
    assertFalse(authorizationService.checkUserHasPerm(Permissions.editProductReferences, "invId"),
      "Debería dar false si no tiene el permiso indicado aunque el inventario sea el correcto");
    assertFalse(authorizationService.checkUserHasPerm(Permissions.addProducts, "anotherInvId"),
      "Debería dar false si el inventario no es el indicado aunque tenga el permiso correcto");
  }

  @Test
  void checkActionIsToLoggedAccount() {
    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned(null, null)).build(),
      null
    ));
    assertFalse(authorizationService.checkAccountIsLogged(),
      "Debería retornar falso si la autenticación en el contexto no tiene nombre de usuario");

    SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
      ContextAuthenticationPrincipal.builder().account(new AccountSigned("username", "accountId")).build(),
      null
    ));
    assertTrue(authorizationService.checkActionIsToLoggedAccount("accountId"), 
      "Debería dar true si la id de la cuenta indicada es igual a la que está en el contexto");
    assertFalse(authorizationService.checkActionIsToLoggedAccount("anotherAccountId"),
      "Debería dar false si la id de la cuenta que quiere hacer la acción no es la que está en el contexto");
  }
}
