package com.nahuelgg.inventory_app.users.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "permission_for_inventory")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class PermissionsForInventoryEntity {
  @Id
  private UUID id;
  // cada permiso estará separado por comas en un solo string, estos provienen del enum Permissions. 
  // Ej: editProducts,editInventory,addProducts
  @Column(nullable = false)
  private String permissions;
  
  @ManyToOne @JoinColumn(nullable = false)
  private UserEntity user;
  @ManyToOne @JoinColumn(nullable = false)
  private InventoryRefEntity inventoryReference;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
