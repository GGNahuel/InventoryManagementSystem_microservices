package com.nahuelgg.inventory_app.users.components;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.JwtService;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@Profile("test")
@RequiredArgsConstructor
public class AuthenticationForTesting {
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder encoder;

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  @Getter @AllArgsConstructor
  public class AuthData {
    private AccountEntity accountSaved;
    private String token;
  }

  public AuthData authenticate(LoginDTO accountInfo) {
    return authenticatePersonalize(null, accountInfo, null, false);
  }

  public AuthData authenticateWithAdminToo(LoginDTO accountInfo) {
    JwtClaimsDTO claimsDto = JwtClaimsDTO.builder()
      .userName("admin")
      .userRole("admin")
      .isAdmin(true)
      .userPerms(List.of())
    .build();

    return authenticatePersonalize(claimsDto, accountInfo, null, true);
  }

  public AuthData authenticateWithUserToo(LoginDTO accountInfo, LoginDTO userInfo, String role, List<PermissionsForInventoryDTO> perms) {
    JwtClaimsDTO claimsDTO = JwtClaimsDTO.builder()
      .userName(userInfo.getUsername())
      .userRole(role)
      .isAdmin(false)
      .userPerms(perms)
    .build();

    return authenticatePersonalize(claimsDTO, accountInfo, userInfo, false);
  }

  /**
   * @param claimsForToken - campo requerido si se quiere iniciar sesión con cualquier usuario
   * @param accountInfo - <I>campo completo requerido</I>
   * @param userInfo - en caso de que se quiera iniciar sesión con un usuario, distinto al de admin.
   * @param logAdmin - en caso de que se quiera iniciar sesión con el admin. Tiene prioridad este valor con respecto
   al <B>userInfo</B>
   * @return Retorna la clase <code>AuthData</code> que contiene:<ul>
    <li><B>accountSaved</B> entidad de la cuenta registrada</li>
    <li><B>token</B> token generado en el login</li>
   </ul>
   */
  public AuthData authenticatePersonalize(JwtClaimsDTO claimsForToken, LoginDTO accountInfo, LoginDTO userInfo, boolean logAdmin) {
    String username = accountInfo.getUsername();
    checkFieldsHasContent(
      new Field("usuario de cuenta", username),
      new Field("contraseña de cuenta", accountInfo.getPassword())
    );

    // Guarda en BDD entidades según los datos ingresados al método
    AccountEntity accountSaved = accountRepository.save(AccountEntity.builder()
      .username(username)
      .password(encoder.encode(accountInfo.getPassword()))
      .inventoriesReferences(new ArrayList<>())
    .build());

    List<UserEntity> usersInAccount = new ArrayList<>();

    UserEntity adminSaved = userRepository.save(UserEntity.builder()
      .name("admin")
      .password(encoder.encode("adminTest"))
      .associatedAccount(accountSaved)
      .isAdmin(true)
      .role("admin")
      .inventoryPerms(new ArrayList<>())
    .build());
    usersInAccount.add(adminSaved);

    // -- En caso de que se quiera hacer login con otro usuario que no sea el admin
    List<PermissionsForInventoryDTO> permsForOtherUser = new ArrayList<>();
    if (userInfo != null && claimsForToken != null) {
      checkFieldsHasContent(
        new Field("nombre de usuario", userInfo.getUsername()),
        new Field("contraseña de usuario", userInfo.getPassword())
      );

      UserEntity otherUser = userRepository.save(UserEntity.builder()
        .name(userInfo.getUsername())
        .password(encoder.encode(userInfo.getPassword()))
        .associatedAccount(accountSaved)
        .isAdmin(false)
        .role(claimsForToken.getUserRole())
        .inventoryPerms(/* claimsForToken.getUserPerms().stream().map(
          permDto -> permsRepo.save()
        ) es innecesario que sea real este atributo en este microservicio*/ new ArrayList<>())
      .build());

      permsForOtherUser = claimsForToken.getUserPerms() != null ? claimsForToken.getUserPerms() : List.of();

      usersInAccount.add(otherUser);
    }

    // -- Agrega el/los usuario/s a la cuenta guardada para que se incluya en el retorno
    accountSaved.setUsers(usersInAccount);

    // Autentica el usuario/cuenta según datos ingresados
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
      ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned(username, accountInfo.getPassword()))
        .user(
          logAdmin ? new UserSigned("admin", "admin", true, List.of()) :
          userInfo != null && !logAdmin ? new UserSigned(userInfo.getUsername(), claimsForToken.getUserRole(), false, permsForOtherUser) :
          null
        )
      .build(),
      accountInfo.getPassword()
    ));

    String accId = accountSaved.getId().toString();
    JwtClaimsDTO completeClaims = claimsForToken != null ? claimsForToken.toBuilder()
      .accountId(accId)
    .build() : JwtClaimsDTO.builder().accountId(accId).isAdmin(false).build();
    return new AuthData(
      accountSaved,
      jwtService.generateToken(completeClaims, username)
    );
  }
}
