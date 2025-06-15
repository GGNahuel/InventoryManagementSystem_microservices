package com.nahuelgg.inventory_app.users.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.nahuelgg.inventory_app.users.controllers.UserController;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.services.AuthorizationService;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.Constants;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
public class Test_UserController {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean UserService service;
  @MockitoBean(name = "authorizationService") AuthorizationService authorizationService;

  UserDTO user = UserDTO.builder().id(UUID.randomUUID().toString()).build();

  private MockHttpSession emulateLoginUser(boolean isAdmin) {
    when(authorizationService.checkUserIsAdmin()).thenReturn(isAdmin);

    MockHttpSession session = new MockHttpSession();
    UserEntity userEntity = UserEntity.builder().isAdmin(isAdmin).build();
    session.setAttribute(Constants.userSessionAttr, userEntity);

    return session;
  }

  @Test
  void getById() throws Exception {
    when(service.getById(UUID.fromString(user.getId()))).thenReturn(user);

    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.get("/user/" + user.getId())).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    UserDTO actual = objectMapper.convertValue(response.getData(), UserDTO.class);

    assertEquals(200, response.getStatus());
    assertEquals(user, actual);
  }

  @Test
  void edit() throws Exception {
    when(service.edit(user)).thenReturn(user);

    MockHttpSession session = emulateLoginUser(true);
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.put("/user")
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(user))
      ).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    assertNull(response.getError());

    UserDTO actual = objectMapper.convertValue(response.getData(), UserDTO.class);

    assertEquals(200, response.getStatus());
    assertEquals(user, actual);
  }

  @Test
  void assignNewPerms() throws Exception {
    PermissionsForInventoryDTO perm = PermissionsForInventoryDTO.builder().id(UUID.randomUUID().toString()).build();
    when(service.assignNewPerms(perm, UUID.fromString(user.getId()))).thenReturn(user);

    MockHttpSession session = emulateLoginUser(true);
    String url = UriComponentsBuilder.fromUriString("/user/add_perms")
      .queryParam("id", user.getId())
    .toUriString();
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.patch(url)
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(perm))
      ).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );
    assertNull(response.getError());

    UserDTO actual = objectMapper.convertValue(response.getData(), UserDTO.class);

    assertEquals(200, response.getStatus());
    assertEquals(user, actual);
  } 

  @Test
  void delete() throws Exception {
    MockHttpSession session = emulateLoginUser(true);
    ResponseDTO response = objectMapper.readValue(
      mockMvc.perform(MockMvcRequestBuilders.delete("/user/" + user.getId()).session(session)).andReturn().getResponse().getContentAsString(),
      ResponseDTO.class
    );

    assertNull(response.getError());
    assertEquals(200, response.getStatus());
    verify(service,times(1)).delete(UUID.fromString(user.getId()));
  }
}
