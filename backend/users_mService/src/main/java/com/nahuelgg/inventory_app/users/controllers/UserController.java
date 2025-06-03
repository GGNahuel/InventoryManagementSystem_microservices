package com.nahuelgg.inventory_app.users.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.Constants;

@RestController
@RequestMapping(Constants.endpointPrefix + "/user")
public class UserController {
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public ResponseEntity<ResponseDTO> getById(@PathVariable String id) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getById(UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @PutMapping("")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> edit(@RequestBody UserDTO user) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.edit(user)),
      HttpStatus.OK
    );
  }

  @PatchMapping("/add_perms")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> assignNewPerms(@RequestBody PermissionsForInventoryDTO perm, @RequestParam String id) throws JsonProcessingException {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.assignNewPerms(perm, UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> delete(@PathVariable String id) {
    service.delete(UUID.fromString(id));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    );
  }

  @PostMapping("/login")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ResponseDTO> login(@RequestParam String id, @RequestParam String pass) {
    service.loginAsUser(UUID.fromString(id), pass);
    return new ResponseEntity<>(
      new ResponseDTO(200, null, "Log-in exitoso"),
      HttpStatus.OK
    );
  }

  @PostMapping("/logout")
  public ResponseEntity<ResponseDTO> logout() {
    service.logoutUser();
    return new ResponseEntity<>(
      new ResponseDTO(200, null, "Log-out exitoso"),
      HttpStatus.OK
    );
  }
}
