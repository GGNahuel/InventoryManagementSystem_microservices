package com.nahuelgg.inventory_app.users.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

  @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER) @JoinColumn(name = "associated_account_id")
  private List<InventoryRefEntity> inventoriesReferences;
  @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER, mappedBy = "associatedAccount")
  private List<UserEntity> users;
}
// TODO: cambiar inventories a ElementCollection, validar cuando se agregan permisos que la id pasada a ese permiso esté dentro de 
// las Ids que se encuentran en la cuenta