package com.nahuelgg.inventory_app.inventories.entities;

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

@Entity(name = "product_in_inv")
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class ProductInInvEntity {
  @Id
  private UUID id;
  @Column(nullable = false)
  private UUID referenceId;
  private Integer stock;
  private Boolean isAvailable;
  @ManyToOne @JoinColumn(nullable = false)
  private InventoryEntity inventory;

  @Override
  public String toString() {
    return String.format(
      "ProductInInvEntity(id: %s, referenceId: %s, stock: %s, isAvailable: %s, inventoryId: %s)",
      this.id != null ? this.id.toString() : "null", this.referenceId.toString(), 
      this.stock != null ? this.stock.toString() : "null", this.isAvailable != null ? this.isAvailable.toString() : "null", 
      this.inventory.getId().toString()
    );
  }

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
