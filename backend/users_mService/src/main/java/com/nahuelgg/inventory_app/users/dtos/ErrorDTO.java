package com.nahuelgg.inventory_app.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ErrorDTO {
  public enum Type {
    warning, critical
  }

  private String message;
  private StackTraceElement[] trackTrace;
  private Type type; 
}
