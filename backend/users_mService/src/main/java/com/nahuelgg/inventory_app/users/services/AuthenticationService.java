package com.nahuelgg.inventory_app.users.services;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.PermsForInv;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final EntityMappers entityMappers = new EntityMappers();

  @Transactional(readOnly = true)
  public TokenDTO login(LoginDTO info) {
    String username = info.getUsername();

    checkFieldsHasContent(new Field("tipo de login", info.isAccountLogin()));
    checkFieldsHasContent(new Field("nombre de cuenta", username), new Field("contraseña", info.getPassword()));
    
    if (!info.isAccountLogin())
    throw new RuntimeException("El tipo de datos enviados no pertenece al login de cuenta");

    Authentication currentAuthInContext = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuthInContext != null)
      throw new RuntimeException("Ya hay una sesión iniciada para la cuenta");
    
    AccountEntity accountToLog = accountRepository.findByUsername(username).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "nombre de usuario", username)
    );
      
    String token = jwtService.generateToken(JwtClaimsDTO.builder().accountId(accountToLog.getId().toString()).build(), username);

    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        ContextAuthenticationPrincipal.builder()
          .account(new AccountSigned(username, info.getPassword()))
          .user(null)
        .build(),
        token
      )
    );

    return new TokenDTO(token);
  }

  @Transactional(readOnly = true)
  public TokenDTO loginAsUser(LoginDTO info) {
    String username = info.getUsername();
    
    checkFieldsHasContent(new Field("tipo de login", info.isAccountLogin()));
    checkFieldsHasContent(new Field("nombre de cuenta", username), new Field("contraseña", info.getPassword()));
    
    if (info.isAccountLogin())
    throw new RuntimeException("El tipo de datos enviados no pertenece al login de usuario");

    Authentication currentAuthInContext = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuthInContext == null)
      throw new RuntimeException("Necesita iniciar sesión como cuenta antes de como usuario");

    ContextAuthenticationPrincipal currentAccountLogged = (ContextAuthenticationPrincipal) currentAuthInContext.getPrincipal();
    if (currentAccountLogged.getUser().getName() != null)
      throw new RuntimeException("Ya hay un usuario en sesión");

    UserEntity userToAuthenticate = userRepository.findByNameAndAccountUsername(username, currentAccountLogged.getUsername()).orElseThrow(
      () -> new ResourceNotFoundException(
        "usuario", "nombre", username + " en la cuenta " + currentAccountLogged.getUsername())
    );
    List<PermissionsForInventoryDTO> permsDto = userToAuthenticate.getInventoryPerms() != null ? userToAuthenticate.getInventoryPerms().stream().map(
      invPermEntity -> entityMappers.mapPerms(invPermEntity)
    ).toList() : null;

    ContextAuthenticationPrincipal loggedAccountAndUser = ContextAuthenticationPrincipal.builder()
      .account(new AccountSigned(currentAccountLogged.getUsername(), currentAccountLogged.getPassword()))
      .user(new UserSigned(
        username, 
        userToAuthenticate.getRole(), 
        userToAuthenticate.getIsAdmin(), 
        permsDto != null ? permsDto.stream().map(
          invPermDto -> new PermsForInv(invPermDto.getIdOfInventoryReferenced(), invPermDto.getPermissions())
        ).toList() : null
      ))
    .build();

    AccountEntity loggedAccount = accountRepository.findByUsername(currentAccountLogged.getUsername()).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "nombre de usuario", username)
    );
    
    String token = jwtService.generateToken(JwtClaimsDTO.builder()
      .accountId(loggedAccount.getId().toString())
      .userName(username)
      .userRole(userToAuthenticate.getRole())
      .isAdmin(userToAuthenticate.getIsAdmin())
      .userPerms(permsDto)
    .build(), currentAccountLogged.getUsername());

    Authentication newAuthInContext = new UsernamePasswordAuthenticationToken(loggedAccountAndUser, token);
    SecurityContextHolder.getContext().setAuthentication(newAuthInContext);
    
    return new TokenDTO(token);
  }

  public TokenDTO logout() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ContextAuthenticationPrincipal)) {
      throw new RuntimeException("No hay cuenta en sesión para cerrar");
    }

    SecurityContextHolder.clearContext();

    String logoutToken = jwtService.generateEmptyToken();
    return new TokenDTO(logoutToken);
  }

  public TokenDTO logoutAsUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ContextAuthenticationPrincipal)) {
      throw new RuntimeException("No hay usuario en sesión para cerrarla");
    }

    ContextAuthenticationPrincipal currentAuth = (ContextAuthenticationPrincipal) auth.getPrincipal();
    String accountUsername = currentAuth.getUsername();

    AccountEntity accountLogged = accountRepository.findByUsername(accountUsername).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "username", accountUsername)
    );

    String token = jwtService.generateToken(JwtClaimsDTO.builder().accountId(accountLogged.getId().toString()).build(), accountUsername);

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
          .account(new AccountSigned(currentAuth.getUsername(), currentAuth.getPassword()))
          .user(null)
        .build(), 
      token
    ));

    return new TokenDTO(token);
  }
}
