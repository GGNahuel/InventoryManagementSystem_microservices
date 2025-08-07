package com.nahuelgg.inventory_app.inventories.entities;

import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserReferenceElement {
  private UUID referenceId;
}
