package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccountFromUsersMSDTO {
  private String id;
  private String username;
  private List<String> idsOfInventoryReferred;
  private List<UserFromUsersMSDTO> users;
}
