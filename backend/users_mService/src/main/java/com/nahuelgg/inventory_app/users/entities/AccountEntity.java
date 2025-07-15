package com.nahuelgg.inventory_app.users.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class AccountEntity {
  @Id @GeneratedValue
  private UUID id;
  @Column(unique = true, nullable = false)
  private String username;
  @Column(nullable = false)
  private String password;

  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true) @JoinColumn(name = "associated_account_id")
  private List<InventoryRefEntity> inventoriesReferences;
  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "associatedAccount")
  private List<UserEntity> users;
}
