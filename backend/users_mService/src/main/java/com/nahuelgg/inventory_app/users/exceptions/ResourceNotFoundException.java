package com.nahuelgg.inventory_app.users.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
    super(String.format("El recurso '%s' con el valor de '%s', para el campo '%s', no se encontró", resourceName, fieldValue, fieldName));
  }
}
