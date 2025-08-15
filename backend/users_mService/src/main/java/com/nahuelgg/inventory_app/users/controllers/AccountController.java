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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.users.dtos.AccountRegistrationDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserRegistrationDTO;
import com.nahuelgg.inventory_app.users.services.AccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
  private final AccountService service;

  /* @GetMapping("") // método para superAdmin
  public ResponseEntity<ResponseDTO> getAll() {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getAll()),
      HttpStatus.OK
    );
  } */

  @GetMapping("/id/{id}")
  @PreAuthorize("@authorizationService.loggedAccountHasTheIdReferenced(#id)")
  public ResponseEntity<ResponseDTO> getById(@PathVariable String id) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getById(UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @PostMapping("/register")
  public ResponseEntity<ResponseDTO> create(@RequestBody AccountRegistrationDTO info) {
    return new ResponseEntity<>(
      new ResponseDTO(201, null, service.create(info)),
      HttpStatus.CREATED
    );
  }

  @PostMapping("/add-user")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> addUser(@RequestBody UserRegistrationDTO user, @RequestParam String accountId) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.addUser(UUID.fromString(accountId), user)),
      HttpStatus.OK
    );
  }

  @PatchMapping("/add-inventory")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> assignInventory(@RequestParam String accountId, @RequestParam String invRefId) {
    service.assignInventory(UUID.fromString(accountId), UUID.fromString(invRefId));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PatchMapping("/remove-inventory")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> removeInventoryAssigned(@RequestParam String accountId, @RequestParam String invRefId) {
    service.removeInventoryAssigned(UUID.fromString(accountId), UUID.fromString(invRefId));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @DeleteMapping("/delete")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#id)")
  public ResponseEntity<ResponseDTO> delete(@RequestParam String id) {
    service.delete(UUID.fromString(id));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, "Cuenta eliminada con éxito"),
      HttpStatus.OK
    );
  }
}
