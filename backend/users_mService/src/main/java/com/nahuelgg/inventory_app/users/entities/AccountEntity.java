package com.nahuelgg.inventory_app.users.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "account")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccountEntity {
  @Id @GeneratedValue
  private UUID id;
  private String username;
  private String password;
  
  @OneToMany @JoinColumn
  private List<InventoryRefEntity> inventoriesReferences;
  @OneToMany @JoinColumn
  private List<UserEntity> users;
}
