package com.nahuelgg.inventory_app.products.exceptions;

public class EmptyFieldException extends RuntimeException {
  public EmptyFieldException(String fieldName) {
    super(String.format("El campo '%s' no puede estar vacío o ser nulo", fieldName));
  }
}
