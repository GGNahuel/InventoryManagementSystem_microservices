package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder
@AllArgsConstructor
public class AccountSessionDTO {
  private String id;
  private String username;
  private List<UserSessionDTO> users;
}
