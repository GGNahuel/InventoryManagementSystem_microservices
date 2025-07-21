package com.nahuelgg.inventory_app.users.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
  @InjectMocks
  private AuthenticationService authenticationService;

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private AccountRepository accountRepository;
  @Mock private UserRepository userRepository;

  private final String username = "testUser";
  private final String password = "testPass";
  private final UUID accountId = UUID.randomUUID();
  private final String token = "tokenTest";

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(authenticationService, "entityMappers", new EntityMappers());
  }
  
  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }
  
  @Test
  void logins_deniedWhenInvalidLoginType() {
    LoginDTO loginToAcc = new LoginDTO(username, password, false);
    LoginDTO loginToUser = new LoginDTO(username, password, true);
    assertThrows(RuntimeException.class, () -> authenticationService.login(loginToAcc));
    assertThrows(RuntimeException.class, () -> authenticationService.login(loginToUser));
  }

  @Test
  void loginAccount_success() {
    LoginDTO login = new LoginDTO(username, password, true);

    AccountEntity acc = new AccountEntity();
    acc.setId(accountId);
    acc.setUsername(username);

    when(accountRepository.findByUsername(username)).thenReturn(Optional.of(acc));
    when(jwtService.generateToken(any(), eq(username))).thenReturn(token);

    TokenDTO response = authenticationService.login(login);

    assertEquals(token, response.getToken());
    verify(authenticationManager).authenticate(any());
    verify(accountRepository).findByUsername(username);
  }

  @Test
  void loginAccount_deniedIfAccountIsAlreadyLogged() {
    LoginDTO login = new LoginDTO(username, password, true);

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));
    assertThrows(RuntimeException.class, () -> authenticationService.login(login));

    verify(authenticationManager, never()).authenticate(any());
    verify(accountRepository, never()).findByUsername(username);
    verify(jwtService, never()).generateToken(any(), any());
  }

  @Test
  void loginAsUser_success() {
    LoginDTO login = new LoginDTO("subUser", password, false);
    ContextAuthenticationPrincipal currentAuth = ContextAuthenticationPrincipal.builder()
      .account(new AccountSigned(username, password))
    .build();

    UserEntity userEntity = UserEntity.builder()
      .name("subUser")
      .role("role")
      .isAdmin(true)
      .associatedAccountId(accountId)
      .inventoryPerms(null)
    .build();

    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(currentAuth, null)
    );

    when(accountRepository.findByUsername(username)).thenReturn(Optional.of(AccountEntity.builder().id(accountId).username(username).build()));
    when(userRepository.findByNameAndAssociatedAccountId("subUser", accountId)).thenReturn(Optional.of(userEntity));
    when(jwtService.generateToken(any(), eq(username))).thenReturn(token);

    TokenDTO result = authenticationService.loginAsUser(login);

    assertEquals(token, result.getToken());
    verify(userRepository).findByNameAndAssociatedAccountId("subUser", accountId);
  }

  @Test
  void loginAsUser_deniedIfUserIsAlreadyLogged() {
    LoginDTO login = new LoginDTO("subUser", password, false);
    ContextAuthenticationPrincipal currentAuth = ContextAuthenticationPrincipal.builder()
      .account(new AccountSigned(username, password))
      .user(new UserSigned("name", "role", false, List.of()))
    .build();

    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(currentAuth, null)
    );

    assertThrows(RuntimeException.class, () -> authenticationService.loginAsUser(login));

    verify(userRepository, never()).findByNameAndAssociatedAccountId(any(), any());
    verify(jwtService, never()).generateToken(any(), any());
  }

  @Test
  void logout_success() {
    ContextAuthenticationPrincipal auth = ContextAuthenticationPrincipal.builder()
      .account(new ContextAuthenticationPrincipal.AccountSigned(username, password))
    .build();

    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(auth, null)
    );

    when(jwtService.generateEmptyToken()).thenReturn(token);

    TokenDTO result = authenticationService.logout();
    assertEquals(token, result.getToken());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void logoutAsUser_success() {
    ContextAuthenticationPrincipal currentAuth = ContextAuthenticationPrincipal.builder()
      .account(new ContextAuthenticationPrincipal.AccountSigned(username, password))
      .user(new ContextAuthenticationPrincipal.UserSigned("subUser", "role", false, List.of()))
    .build();

    AccountEntity accountEntity = new AccountEntity();
    accountEntity.setId(accountId);
    accountEntity.setUsername(username);

    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(currentAuth, null)
    );

    when(accountRepository.findByUsername(username)).thenReturn(Optional.of(accountEntity));
    when(jwtService.generateToken(any(), eq(username))).thenReturn(token);

    TokenDTO result = authenticationService.logoutAsUser();

    assertEquals(token, result.getToken());
  }
}
