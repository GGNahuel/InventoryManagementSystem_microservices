package com.nahuelgg.inventory_app.products.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "product")
@Data @Builder
@AllArgsConstructor @NoArgsConstructor
public class ProductEntity {
  @Id @GeneratedValue
  private UUID id;
  @Column(nullable = false)
  private String name;
  private String brand;
  private String model;
  private String description;
  @Column(nullable = false)
  private Double unitPrice;

  @ManyToMany @JoinTable(
    name = "product_categories", 
    joinColumns = @JoinColumn(referencedColumnName = "id"), // id de productos
    inverseJoinColumns = @JoinColumn(referencedColumnName = "id") // ide de categorías
  )
  private List<CategoryEntity> categories;
}
