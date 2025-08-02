package com.nahuelgg.inventory_app.inventories.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "inventory")
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class InventoryEntity {
  @Id @GeneratedValue
  private UUID id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private UUID accountId;

  @ManyToMany(fetch = FetchType.EAGER) @JoinTable(
    name = "inventory_users", 
    // id del inventario
    joinColumns = @JoinColumn(referencedColumnName = "id"), 
    // id de la ref al usuario (no la id de referencia, sino la de la tabla con las referencias)
    inverseJoinColumns = @JoinColumn(referencedColumnName = "id")
  )
  private List<UserReferenceEntity> users;
  @OneToMany(mappedBy = "inventory", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
  private List<ProductInInvEntity> products;
}
