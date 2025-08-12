package com.nahuelgg.inventory_app.products.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
  private Double unitPrice;
  // Será un solo string, pero cada categoría estará separada con "," para hacer luego el mapeo en los dto a una lista de strings
  private String categories;
  @Column(nullable = false)
  private UUID accountId;
}
