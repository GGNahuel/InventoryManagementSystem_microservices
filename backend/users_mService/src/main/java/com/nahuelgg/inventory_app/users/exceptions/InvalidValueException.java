package com.nahuelgg.inventory_app.users.exceptions;

public class InvalidValueException extends RuntimeException {
  public InvalidValueException(String message) {
    super(message);
  }
}
