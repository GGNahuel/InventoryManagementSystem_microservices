package com.nahuelgg.inventory_app.users.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTest {
  @Mock AccountRepository accountRepository;

  @InjectMocks AuthorizationService service;

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

    Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, null);
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
    Authentication auth = new UsernamePasswordAuthenticationToken("string-user", null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertFalse(service.checkUserIsAdmin());
  }

  @Test
  void checkUserIsAdmin_deniedIfUserIsNull() {
    ContextAuthenticationPrincipal principal = ContextAuthenticationPrincipal.builder()
      .user(null)
    .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertFalse(service.checkUserIsAdmin());
  }

  @Test
  void loggedAccountHasTheIdReferenced_success() {
    UUID idToCompare = UUID.randomUUID();
    String username = "username";
    when(accountRepository.findByUsername(username)).thenReturn(Optional.of(AccountEntity.builder()
      .id(idToCompare)
      .username(username)
    .build()));

    Authentication auth = new UsernamePasswordAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned(username, "1234"))
        .user(null)
      .build(), 
      "1234"
    );
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertTrue(service.loggedAccountHasTheIdReferenced(idToCompare.toString()));
    verify(accountRepository).findByUsername(username);
  }

  @Test
  void loggedAccountHasTheIdReferenced_deniedIfNotLogged() {
    SecurityContextHolder.clearContext();
    assertFalse(service.loggedAccountHasTheIdReferenced(UUID.randomUUID().toString()));
  }

  @Test
  void loggedAccountHasTheIdReferenced_deniedIfIdsAreNotEqual() {
    UUID idToCompare = UUID.randomUUID();
    String loggedAccountUsername = "username";
    when(accountRepository.findByUsername(loggedAccountUsername)).thenReturn(Optional.of(AccountEntity.builder()
      .id(UUID.randomUUID())
      .username(loggedAccountUsername)
    .build()));

    Authentication auth = new UsernamePasswordAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned(loggedAccountUsername, "1234"))
        .user(null)
      .build(), 
      "1234"
    );
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertFalse(service.loggedAccountHasTheIdReferenced(idToCompare.toString()));
    verify(accountRepository).findByUsername(loggedAccountUsername);
  }
}
