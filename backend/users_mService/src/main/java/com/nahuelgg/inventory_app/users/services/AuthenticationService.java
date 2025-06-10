package com.nahuelgg.inventory_app.users.services;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;


  public String login(LoginDTO info) {
    checkFieldsHasContent(new Field("tipo de login", info.isAccountLogin()));
    if (!info.isAccountLogin())
    throw new RuntimeException("El tipo de datos enviados no pertenece al login de cuenta");
    
    String username = info.getUsername();
    checkFieldsHasContent(new Field("nombre de cuenta", username), new Field("contraseña", info.getPassword()));
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
}
