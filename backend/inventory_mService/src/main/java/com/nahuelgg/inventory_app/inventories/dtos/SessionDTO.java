package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO.InventoryPermsDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class SessionDTO {
  private AccountSession account;
  private UserSession user;

  @Data
  public class AccountSession {
    private String id;
    private String username;
    private List<UserSession> users;
  }

  @Data
  public class UserSession {
    private String id;
    private String name;
    private String role;
    private Boolean isAdmin;
    private List<InventoryPermsDTO> inventoryPerms;
  }
}
