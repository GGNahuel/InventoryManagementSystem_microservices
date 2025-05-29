package com.nahuelgg.inventory_app.users.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.nahuelgg.inventory_app.users.dtos.ErrorDTO;
import com.nahuelgg.inventory_app.users.dtos.ResponseDTO;

@RestControllerAdvice
public class _ExceptionsHandler {
  private ResponseDTO buildResponseDTO(Integer status, Exception ex, ErrorDTO.Type type, String specialMessage) {
    ErrorDTO errorDTO = new ErrorDTO(ex.getMessage(), ex.getStackTrace(), type);
    ResponseDTO response = new ResponseDTO(
      status,
      errorDTO,
      specialMessage == null ? ex.getMessage() : specialMessage
    );

    return response;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ResponseDTO> resourceNotFound(ResourceNotFoundException ex) {
    return new ResponseEntity<>(buildResponseDTO(404, ex, ErrorDTO.Type.critical, null), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EmptyFieldException.class)
  public ResponseEntity<ResponseDTO> emptyField(EmptyFieldException ex) {
    return new ResponseEntity<>(buildResponseDTO(406, ex, ErrorDTO.Type.warning, null), HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(InvalidValueException.class)
  public ResponseEntity<ResponseDTO> invalidValue(InvalidValueException ex) {
    return new ResponseEntity<>(buildResponseDTO(400, ex, ErrorDTO.Type.warning, null), HttpStatus.BAD_REQUEST);
  }

  // INPUTS IN CONTROLLERS JAVA EXCEPTIONS
  @ExceptionHandler(UnrecognizedPropertyException.class)
  public ResponseEntity<ResponseDTO> handleUnknownProperty(UnrecognizedPropertyException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Campo no reconocido: " + ex.getPropertyName()),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MismatchedInputException.class)
  public ResponseEntity<ResponseDTO> handleMismatchedType(MismatchedInputException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Error de tipo en el body: " + ex.getOriginalMessage()),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseDTO> handleParamTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Tipo incorrecto en parámetro: " + ex.getName()),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ResponseDTO> handleMissingParam(MissingServletRequestParameterException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Falta el parámetro: " + ex.getParameterName()),
      HttpStatus.BAD_REQUEST
    );
  }

  // OTHER EXCEPTIONS
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseDTO> generalExceptions(Exception ex) {
    return new ResponseEntity<>(
      buildResponseDTO(500, ex, ErrorDTO.Type.critical, "Ocurrió un error inesperado, intente de nuevo más tarde"),
      HttpStatus.INTERNAL_SERVER_ERROR
    );
  }
}
