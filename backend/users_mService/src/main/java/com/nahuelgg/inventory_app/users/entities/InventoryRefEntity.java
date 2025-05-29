package com.nahuelgg.inventory_app.users.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "inventoryReference")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class InventoryRefEntity {
  @Id @GeneratedValue
  private UUID id;
  @Column(nullable = false, unique = true)
  private String inventoryIdReference;
}
