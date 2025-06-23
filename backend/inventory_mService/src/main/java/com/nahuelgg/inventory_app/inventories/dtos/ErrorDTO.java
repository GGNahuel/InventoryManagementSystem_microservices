package com.nahuelgg.inventory_app.inventories.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ErrorDTO {
  public enum Type {
    warning, critical
  }

  private String message;
  private String cause;
  private Type type; 
  private String exClass;
}
