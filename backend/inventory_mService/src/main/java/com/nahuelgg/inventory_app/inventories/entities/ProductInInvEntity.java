package com.nahuelgg.inventory_app.inventories.entities;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductInInvEntity {
  @Id @GeneratedValue
  private UUID id;
  private UUID referenceId;
  private Integer stock;
  private Boolean isAvailable;
}
