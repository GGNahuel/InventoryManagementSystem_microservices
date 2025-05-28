package com.nahuelgg.inventory_app.products.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resourceName, String searchField, String searchFieldValue) {
    super(String.format("No se encontró %s con %s '%s'", resourceName, searchField, searchFieldValue));
  }
}
