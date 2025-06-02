package com.nahuelgg.inventory_app.users.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "users")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserEntity {
  @Id @GeneratedValue
  private UUID id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String password;
  @Column(nullable = false)
  private String role;
  @Column(nullable = false)
  private Boolean isAdmin;
  
  @ManyToOne @JoinColumn
  private AccountEntity associatedAccount;  
  @OneToMany(cascade = CascadeType.REMOVE) @JoinColumn
  private List<PermissionsForInventoryEntity> inventoryPerms;
}
