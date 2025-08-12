package com.nahuelgg.inventory_app.products.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
  @ElementCollection(fetch = FetchType.EAGER) // TODO: cambiarlo a string y formatearlo con comas, como es con los permisos en usuarios
  private List<String> categories;
  @Column(nullable = false)
  private UUID accountId;
}
