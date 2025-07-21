package com.nahuelgg.inventory_app.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class LoginDTO {
  private String username;
  private String password;
  private boolean accountLogin;
}
