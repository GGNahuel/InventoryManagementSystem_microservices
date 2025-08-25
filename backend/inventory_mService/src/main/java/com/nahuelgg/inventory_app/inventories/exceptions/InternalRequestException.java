package com.nahuelgg.inventory_app.inventories.exceptions;

import lombok.Getter;

@Getter
public class InternalRequestException extends RuntimeException {
  private String internalRequestMessage;
  private String internalRequestResponse;

  public InternalRequestException(String msg, String internalRequestResponse) {
    super("Un llamado interno no fue exitoso. Mensaje de la operación: " + msg);
    this.internalRequestMessage = msg;
    this.internalRequestResponse = internalRequestResponse;
  }
}
