package com.nahuelgg.inventory_app.users.services;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
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

  @Transactional
  public String login(LoginDTO info) {
    String username = info.getUsername();

    checkFieldsHasContent(new Field("tipo de login", info.isAccountLogin()));
    checkFieldsHasContent(new Field("nombre de cuenta", username), new Field("contraseña", info.getPassword()));
    
    if (!info.isAccountLogin())
    throw new RuntimeException("El tipo de datos enviados no pertenece al login de cuenta");
    
    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        username,
        info.getPassword()
      )
    );

    accountRepository.findByUsername(username).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "nombre de usuario", username)
    );

    String token = jwtService.generateToken(JwtClaimsDTO.builder().accountUsername(username).build());

    return token;
  }

  @Transactional
  public String loginAsUser(LoginDTO info) {
    String username = info.getUsername();
    
    checkFieldsHasContent(new Field("tipo de login", info.isAccountLogin()));
    checkFieldsHasContent(new Field("nombre de cuenta", username), new Field("contraseña", info.getPassword()));
    
    if (info.isAccountLogin())
    throw new RuntimeException("El tipo de datos enviados no pertenece al login de usuario");

    Authentication currentAuthInContext = SecurityContextHolder.getContext().getAuthentication();
    UserDetails currentAccountLogged = (UserDetails) currentAuthInContext.getPrincipal();

    UserEntity userToAuthenticate = userRepository.findByNameAndAccountUsername(username, currentAccountLogged.getUsername()).orElseThrow(
      () -> new ResourceNotFoundException(
        "usuario", "nombre", username + " en la cuenta " + currentAccountLogged.getUsername())
    );
    List<PermissionsForInventoryDTO> permsDto = userToAuthenticate.getInventoryPerms().stream().map(
      invPermEntity -> entityMappers.mapPerms(invPermEntity)
    ).toList();

    ContextAuthenticationPrincipal loggedAccountAndUser = ContextAuthenticationPrincipal.builder()
      .account(new AccountSigned(currentAccountLogged.getUsername(), currentAccountLogged.getPassword()))
      .user(new UserSigned(
        username, 
        userToAuthenticate.getRole(), 
        userToAuthenticate.getIsAdmin(), 
        permsDto.stream().map(
          invPermDto -> new PermsForInv(invPermDto.getIdOfInventoryReferenced(), invPermDto.getPermissions())
        ).toList()
      ))
    .build();

    Authentication newAuthInContext = new UsernamePasswordAuthenticationToken(loggedAccountAndUser, null);
    SecurityContextHolder.getContext().setAuthentication(newAuthInContext);

    return jwtService.generateToken(JwtClaimsDTO.builder()
      .accountUsername(currentAccountLogged.getUsername())
      .userName(username)
      .userRole(userToAuthenticate.getRole())
      .isAdmin(userToAuthenticate.getIsAdmin())
      .userPerms(permsDto)
    .build());
  }
}
