package com.nahuelgg.inventory_app.inventories.dtos;

import java.util.List;

import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO.InventoryPermsDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class JwtClaimsDTO {
  private String accountId;
  private String userName;
  private String userRole;
  private boolean isAdmin;
  private List<InventoryPermsDTO> userPerms;
}
