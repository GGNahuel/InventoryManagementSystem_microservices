package com.nahuelgg.inventory_app.users.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "inventory_reference")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class InventoryRefEntity {
  @Id
  private UUID id;
  @Column(nullable = false, unique = true)
  private UUID inventoryIdReference;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
