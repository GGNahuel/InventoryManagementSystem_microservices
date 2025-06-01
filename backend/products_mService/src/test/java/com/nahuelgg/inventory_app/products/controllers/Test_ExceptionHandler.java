package com.nahuelgg.inventory_app.products.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.exceptions.EmptyFieldException;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.products.services.CategoryService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test_ExceptionHandler {
  @Autowired TestRestTemplate restTemplate;

  @MockitoBean CategoryService service;

  @Test
  void emptyFieldEx() {
    when(service.getAll()).thenThrow(EmptyFieldException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(406), response.getStatusCode());
  }

  @Test
  void resourceNotFound() {
    when(service.getAll()).thenThrow(ResourceNotFoundException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }
  
  @Test
  void unknownProperty() {
    when(service.getAll()).thenThrow(UnrecognizedPropertyException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }
  @Test
  void mismatchedType() {
    when(service.getAll()).thenThrow(MismatchedInputException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }
  @Test
  void paramTypeMismatch() {
    when(service.getAll()).thenThrow(MethodArgumentTypeMismatchException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }
  @Test
  void missingParam() {
    when(service.getAll()).thenThrow(MissingServletRequestParameterException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }
  
  @Test
  void globalException() {
    when(service.getAll()).thenThrow(RuntimeException.class);
    ResponseEntity<ResponseDTO> response = restTemplate.getForEntity("/category", ResponseDTO.class);
    assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
  }
}
