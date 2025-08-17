package com.nahuelgg.inventory_app.products.entities;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
  @Id @GeneratedValue @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  private UUID id;
  @Column(nullable = false)
  private String name;
  private String brand;
  private String model;
  private String description;
  private Double unitPrice;
  @ElementCollection(fetch = FetchType.EAGER)
  private List<String> categories;
  @Column(nullable = false)
  private UUID accountId;
}
