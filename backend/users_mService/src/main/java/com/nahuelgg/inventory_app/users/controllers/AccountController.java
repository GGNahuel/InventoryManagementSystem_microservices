package com.nahuelgg.inventory_app.users.controllers;

import java.util.HashMap;
import java.util.Map;
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

import com.nahuelgg.inventory_app.users.dtos.AccountSessionDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.dtos.UserSessionDTO;
import com.nahuelgg.inventory_app.users.services.AccountService;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.Constants;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping(Constants.endpointPrefix + "/account")
public class AccountController {
  private final AccountService service;
  @SuppressWarnings("unused")
  private final UserService userService;

  public AccountController(AccountService service, UserService userService) {
    this.service = service;
    this.userService = userService;
  }

  @GetMapping("")
  public ResponseEntity<ResponseDTO> getAll() {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getAll()),
      HttpStatus.OK
    );
  }

  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> session(HttpSession session) {
    System.out.println(session.getAttribute(Constants.accountSessionAttr));
    AccountSessionDTO accountLogged = (AccountSessionDTO) session.getAttribute(Constants.accountSessionAttr);
    if (accountLogged == null) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    UserSessionDTO userLogged = (UserSessionDTO) session.getAttribute(Constants.userSessionAttr);
    System.out.println(accountLogged.toString());
    System.out.println("_______________" + session.getAttribute(Constants.accountSessionAttr));
    Map<String, Object> data = new HashMap<>();
    data.put("account", accountLogged);
    data.put("user", userLogged);

    return new ResponseEntity<>(
      data,
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<ResponseDTO> getById(@PathVariable String id) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.getById(UUID.fromString(id))),
      HttpStatus.OK
    );
  }

  @PostMapping("")
  public ResponseEntity<ResponseDTO> create(
    @RequestParam String username, @RequestParam String password, @RequestParam String passwordRepeated, 
    @RequestParam String adminPassword, @RequestParam String adminPasswordRepeated
  ) {
    return new ResponseEntity<>(
      new ResponseDTO(201, null, service.create(username, password, passwordRepeated, adminPassword, adminPasswordRepeated)),
      HttpStatus.CREATED
    );
  }

  @PostMapping("/add_user")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> addUser(
    @RequestBody UserDTO user, @RequestParam String accountId,
    @RequestParam String password, @RequestParam String passwordRepeated
  ) {
    return new ResponseEntity<>(
      new ResponseDTO(200, null, service.addUser(user, UUID.fromString(accountId), password, passwordRepeated)),
      HttpStatus.OK
    );
  }

  @PatchMapping("/add_inventory")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> assignInventory(@RequestParam String accountId, @RequestParam String invId) {
    service.assignInventory(UUID.fromString(accountId), UUID.fromString(invId));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    );
  }

  @PatchMapping("/remove_inventory")
  @PreAuthorize("@userService_Impl.checkUserIsAdmin()")
  public ResponseEntity<ResponseDTO> removeInventoryAssigned(@RequestParam String accountId, @RequestParam String invId) {
    service.removeInventoryAssigned(UUID.fromString(accountId), UUID.fromString(invId));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    );
  }

  @DeleteMapping("")
  public ResponseEntity<ResponseDTO> delete(@RequestParam String id) {
    service.delete(UUID.fromString(id));
    return new ResponseEntity<>(
      new ResponseDTO(200, null, "Cuenta eliminada con éxito"),
      HttpStatus.OK
    );
  }
}
