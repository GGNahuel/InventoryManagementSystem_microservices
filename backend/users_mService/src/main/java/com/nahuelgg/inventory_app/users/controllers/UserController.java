package com.nahuelgg.inventory_app.users.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(Constants.endpointPrefix + "/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService service;

  @GetMapping("/id/{id}")
  @PreAuthorize("@authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> getById(@PathVariable String id, @RequestParam String accountId) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getById(UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @PutMapping("/edit")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> edit(@RequestBody UserDTO user, @RequestParam String accountId) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.edit(user)),
      HttpStatus.OK
    );
  }

  @PatchMapping("/add-perms")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> assignNewPerms(@RequestBody PermissionsForInventoryDTO perm, @RequestParam String id, @RequestParam String accountId)
  throws JsonProcessingException {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.assignNewPerms(perm, UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.loggedAccountHasTheIdReferenced(#accountId)")
  public ResponseEntity<ResponseDTO> delete(@PathVariable String id, @RequestParam String accountId) {
    service.delete(UUID.fromString(id));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    );
  }
}
