package com.nahuelgg.inventory_app.users.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "users")
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class UserEntity {
  @Id
  private UUID id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String password;
  @Column(nullable = false)
  private String role;
  @Column(nullable = false)
  private Boolean isAdmin;
  
  @ManyToOne @JoinColumn(name = "associated_account_id", nullable = false)
  private AccountEntity associatedAccount;  
  @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER) @JoinColumn(name = "associated_user_id")
  private List<PermissionsForInventoryEntity> inventoryPerms;

  @Override
  public String toString() {
    return "UserEntity(id: %s, name: %s, password: %s, role: %s, isAdmin: %s, associatedAccountId: %s, inventoryPerms: %s)"
      .formatted(
        this.id.toString(), this.name, this.password, this.role, this.isAdmin.toString(), 
        this.associatedAccount.getId(), this.inventoryPerms.toString()
      );
  }

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
