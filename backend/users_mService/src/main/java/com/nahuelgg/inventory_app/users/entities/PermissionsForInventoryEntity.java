package com.nahuelgg.inventory_app.users.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "permission_for_inventory")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class PermissionsForInventoryEntity {
  @Id @GeneratedValue @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  private UUID id;
  // cada permiso estará separado por comas y espacio en un solo string, estos provienen del enum Permissions. 
  // Ej: editProducts, editInventory, etc
  @Column(nullable = false)
  private String permissions;
  
  @ManyToOne @JoinColumn(nullable = false)
  private InventoryRefEntity inventoryReference;
}
