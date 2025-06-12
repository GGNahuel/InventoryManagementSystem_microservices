package com.nahuelgg.inventory_app.users.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.configs.SecurityConfig;
import com.nahuelgg.inventory_app.users.controllers.AccountController;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.services.AccountService;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.Constants;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
public class Test_AccountController {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean AccountService service;
  @MockitoBean(name = "userService_Impl") UserService userService;

  AccountDTO acc = AccountDTO.builder()
    .id(UUID.randomUUID().toString())
    .username("account")
    .users(new ArrayList<>())
  .build();

  private MockHttpSession emulateLoginUser(boolean isAdmin) {
    when(userService.checkUserIsAdmin()).thenReturn(isAdmin);

    MockHttpSession session = new MockHttpSession();
    UserEntity userEntity = UserEntity.builder().isAdmin(isAdmin).build();
    session.setAttribute(Constants.userSessionAttr, userEntity);

    return session;
  }

  @Test
  void getById() throws Exception {
    when(service.getById(UUID.fromString(acc.getId()))).thenReturn(acc);

    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.get("/account/" + acc.getId())).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    AccountDTO actual = objectMapper.convertValue(response.getData(), AccountDTO.class);

    assertEquals(200, response.getStatus());
    assertEquals(acc, actual);
  }

  @Test
  void create() throws Exception {
    String username = "user";
    String password = "password";
    String adminPassword = "adminPassword";
    when(service.create(username, password, password, adminPassword, adminPassword)).thenReturn(acc);

    String url = UriComponentsBuilder.fromUriString("/account")
      .queryParam("username", username)
      .queryParam("password", password)
      .queryParam("passwordRepeated", password)
      .queryParam("adminPassword", adminPassword)
      .queryParam("adminPasswordRepeated", adminPassword)
    .toUriString();
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.post(url)).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    assertNull(response.getError());
    assertEquals(201, response.getStatus());
    
    AccountDTO actual = objectMapper.convertValue(response.getData(), AccountDTO.class);
    assertEquals(acc, actual);
  }

  @Test
  void addUser() throws Exception {
    UserDTO user = UserDTO.builder().name("user").build();
    when(service.addUser(user, UUID.fromString(acc.getId()), "123", "123")).thenReturn(user);
    
    String url = UriComponentsBuilder.fromUriString("/account/add_user")
      .queryParam("accountId", acc.getId())
      .queryParam("password", "123")
      .queryParam("passwordRepeated", "123")
    .toUriString();
    String body = objectMapper.writeValueAsString(user);

    MockHttpSession session = emulateLoginUser(true);
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.post(url)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .session(session)
      ).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    
    assertNull(response.getError());
    assertEquals(200, response.getStatus());

    UserDTO actual = objectMapper.convertValue(response.getData(), UserDTO.class);
    assertEquals(user, actual);
  }

  @Test
  void assignInventory() throws Exception {
    UUID invId = UUID.randomUUID();
    MockHttpSession session = emulateLoginUser(true);
    String url = UriComponentsBuilder.fromUriString("/account/add_inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.patch(url)
        .session(session)
      ).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );

    assertNull(response.getError());
    assertEquals(200, response.getStatus());
    verify(service,times(1)).assignInventory(UUID.fromString(acc.getId()), invId);
  }

  @Test
  void removeInventoryAssigned() throws Exception {
    UUID invId = UUID.randomUUID();
    MockHttpSession session = emulateLoginUser(true);
    String url = UriComponentsBuilder.fromUriString("/account/remove_inventory")
      .queryParam("accountId", acc.getId())
      .queryParam("invId", invId.toString())
    .toUriString();
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.patch(url)
        .session(session)
      ).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );

    assertNull(response.getError());
    assertEquals(200, response.getStatus());
    verify(service,times(1)).removeInventoryAssigned(UUID.fromString(acc.getId()), invId);
  }

  @Test
  void delete() throws Exception {
    String url = UriComponentsBuilder.fromUriString("/account")
      .queryParam("id", acc.getId())
    .toUriString();
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.delete(url)).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );

    assertNull(response.getError());
    assertEquals(200, response.getStatus());
    verify(service,times(1)).delete(UUID.fromString(acc.getId()));
  }
}
