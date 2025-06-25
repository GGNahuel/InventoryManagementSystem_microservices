package com.nahuelgg.inventory_app.users.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.users.dtos.LoginDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.services.AuthenticationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/authenticate")
@RequiredArgsConstructor
public class AuthenticationController {
  private final AuthenticationService authenticationService;

  @PostMapping("/login/account")
  public ResponseEntity<TokenDTO> login(@RequestBody LoginDTO info) {
    return new ResponseEntity<>(authenticationService.login(info), HttpStatus.OK);
  }

  @PostMapping("/login/user")
  public ResponseEntity<TokenDTO> loginAsUser(@RequestBody LoginDTO info) {
    return new ResponseEntity<>(authenticationService.loginAsUser(info), HttpStatus.OK);
  }

  @PostMapping("/logout/account")
  public ResponseEntity<TokenDTO> logout() {
    return new ResponseEntity<>(authenticationService.logout(), HttpStatus.OK);
  }

  @PostMapping("/logout/user")
  public ResponseEntity<TokenDTO> logoutAsUser() {
    return new ResponseEntity<>(authenticationService.logoutAsUser(), HttpStatus.OK);
  }
}
