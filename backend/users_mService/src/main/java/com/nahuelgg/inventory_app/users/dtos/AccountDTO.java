package com.nahuelgg.inventory_app.users.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class AccountDTO {
  private String id;
  private String username;
  private String nickName;
  private List<String> inventoryReferenceIds;
  private List<UserDTO> users;
}
