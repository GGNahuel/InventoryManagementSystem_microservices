package com.nahuelgg.inventory_app.inventories.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
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

@Entity
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class InventoryEntity {
  @Id @GeneratedValue
  private UUID id;
  private String name;
  private UUID accountId;

  @ManyToMany @JoinTable(
    name = "product_categories", 
    joinColumns = @JoinColumn(referencedColumnName = "id"), // id del inventario
    inverseJoinColumns = @JoinColumn(referencedColumnName = "id") // id de la ref al usuario
  )
  private List<UserReferenceEntity> users;
  @OneToMany @JoinColumn
  private List<ProductInInvEntity> products;
}
