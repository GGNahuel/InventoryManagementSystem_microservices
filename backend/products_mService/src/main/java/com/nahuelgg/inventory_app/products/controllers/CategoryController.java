package com.nahuelgg.inventory_app.products.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.products.dtos.CategoryDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.services.CategoryService;
import com.nahuelgg.inventory_app.products.utilities.Constants;

@RestController
@RequestMapping(Constants.endpointPrefix + "/category")
public class CategoryController {
  private final CategoryService service;

  public CategoryController(CategoryService service) {
    this.service = service;
  }

  @GetMapping("")
  public ResponseEntity<ResponseDTO> getAll() {
    ResponseDTO response = new ResponseDTO(200, null, service.getAll());

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/name/{name}")
  public ResponseEntity<ResponseDTO> getByName(@PathVariable String name) {
    ResponseDTO response = new ResponseDTO(200, null, service.getByName(name));

    return new ResponseEntity<>(response, HttpStatus.OK); 
  }

  @GetMapping("/id/{id}")
  public ResponseEntity<ResponseDTO> getById(@PathVariable String id) {
    ResponseDTO response = new ResponseDTO(200, null, service.getById(UUID.fromString(id)));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("")
  public ResponseEntity<ResponseDTO> create(@RequestParam String name) {
    ResponseDTO response = new ResponseDTO(201, null, service.create(name));

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PutMapping("")
  public ResponseEntity<ResponseDTO> update(@RequestBody CategoryDTO category) {
    service.update(category);
    ResponseDTO response = new ResponseDTO(200, null, "Categoría actualizada con éxito");

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @DeleteMapping("")
    public ResponseEntity<ResponseDTO> delete(@RequestParam String id) {
    service.delete(UUID.fromString(id));
    ResponseDTO response = new ResponseDTO(200, null, "Categoría eliminada con éxito");

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
