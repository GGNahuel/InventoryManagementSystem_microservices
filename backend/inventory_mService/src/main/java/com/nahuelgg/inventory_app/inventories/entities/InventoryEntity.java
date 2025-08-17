package com.nahuelgg.inventory_app.inventories.entities;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "inventory")
@Data @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class InventoryEntity {
  @Id @GeneratedValue @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  private UUID id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private UUID accountId;

  @OneToMany(mappedBy = "inventory", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
  private List<ProductInInvEntity> products;
}
