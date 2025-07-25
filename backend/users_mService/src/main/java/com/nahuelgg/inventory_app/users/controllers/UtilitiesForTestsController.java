package com.nahuelgg.inventory_app.users.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Profile("test")
public class UtilitiesForTestsController {
  @GetMapping("/security-context")
  public ResponseEntity<Authentication> getContext() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    return new ResponseEntity<>(auth, HttpStatus.OK);
  }
}
