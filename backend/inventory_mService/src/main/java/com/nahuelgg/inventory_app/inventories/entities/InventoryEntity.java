package com.nahuelgg.inventory_app.inventories.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "inventory")
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class InventoryEntity {
  @Id
  private UUID id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private UUID accountId;

  @OneToMany(mappedBy = "inventory", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
  private List<ProductInInvEntity> products;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
