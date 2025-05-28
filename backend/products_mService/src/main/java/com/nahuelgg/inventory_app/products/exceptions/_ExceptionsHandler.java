package com.nahuelgg.inventory_app.products.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nahuelgg.inventory_app.products.dtos.ErrorDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;

@RestControllerAdvice
public class _ExceptionsHandler {
  private ResponseDTO buildResponseDTO(Integer status, Exception ex, ErrorDTO.Type type) {
    ErrorDTO errorDTO = new ErrorDTO(ex.getMessage(), ex.getStackTrace(), type);
    ResponseDTO response = new ResponseDTO(
      status,
      errorDTO,
      status != 500 ? ex.getMessage() : "Ocurrió un error inesperado, intente de nuevo más tarde"
    );

    return response;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ResponseDTO> resourceNotFound(ResourceNotFoundException ex) {
    return new ResponseEntity<>(buildResponseDTO(404, ex, ErrorDTO.Type.critical), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EmptyFieldException.class)
  public ResponseEntity<ResponseDTO> emptyField(EmptyFieldException ex) {
    return new ResponseEntity<>(buildResponseDTO(406, ex, ErrorDTO.Type.warning), HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseDTO> generalExceptions(Exception ex) {
    return new ResponseEntity<>(buildResponseDTO(500, ex, ErrorDTO.Type.critical), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
