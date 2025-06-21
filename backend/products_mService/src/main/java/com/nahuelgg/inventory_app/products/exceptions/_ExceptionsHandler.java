package com.nahuelgg.inventory_app.products.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.nahuelgg.inventory_app.products.dtos.ErrorDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;

@RestControllerAdvice
public class _ExceptionsHandler {
  private ResponseDTO buildResponseDTO(Integer status, Exception ex, ErrorDTO.Type type, String specialMessage) {
    ErrorDTO errorDTO = new ErrorDTO(ex.getMessage(), ex.getCause() != null ? ex.getCause().toString() : null, type, ex.getClass().toString());
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

  // INPUTS IN CONTROLLERS JAVA EXCEPTIONS
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ResponseDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getCause();

    if (cause instanceof MismatchedInputException) {
      return new ResponseEntity<>(
        buildResponseDTO(400, (Exception) cause, null, "Error de tipo en el body: " + cause.getMessage()),
        HttpStatus.BAD_REQUEST
      );
    }

    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Error en el cuerpo de la solicitud"),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseDTO> paramTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Tipo incorrecto en parámetro: " + ex.getName()),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ResponseDTO> missingParam(MissingServletRequestParameterException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(400, ex, null, "Falta el parámetro: " + ex.getParameterName()),
      HttpStatus.BAD_REQUEST
    );
  }

  // AUTHORIZATION EXCEPTIONS
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ResponseDTO> authorizationDenied(AuthorizationDeniedException ex) {
    return new ResponseEntity<>(
      buildResponseDTO(403, ex, ErrorDTO.Type.critical, "No tiene los permisos para acceder a esta acción/recurso."),
      HttpStatus.FORBIDDEN
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
