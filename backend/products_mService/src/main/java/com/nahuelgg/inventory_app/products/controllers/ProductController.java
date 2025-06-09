package com.nahuelgg.inventory_app.products.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.services.ProductService;
import com.nahuelgg.inventory_app.products.utilities.Constants;

@RestController
@RequestMapping(Constants.endpointPrefix + "/product")
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping("/ids")
  public ResponseEntity<ResponseDTO> getByIds(@RequestParam List<String> list) {
    List<UUID> uuidList = list.stream().map(string -> UUID.fromString(string)).toList();
    ResponseDTO response = new ResponseDTO(200, null, service.getByIds(uuidList));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/search")
  public ResponseEntity<ResponseDTO> search(
    @RequestParam(required = false) String brand,
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String model,
    @RequestParam(required = false) List<String> categoryNames,
    @RequestParam String accountId
  ) {
    ResponseDTO response = new ResponseDTO(200, null, service.search(brand, name, model, categoryNames, UUID.fromString(accountId)));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("")
  public ResponseEntity<ResponseDTO> create(@RequestBody ProductDTO product) {
    ResponseDTO response = new ResponseDTO(201, null, service.create(product));

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PutMapping("/edit")
  public ResponseEntity<ResponseDTO> update(@RequestBody ProductDTO product) {
    ResponseDTO response = new ResponseDTO(200, null, service.update(product));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @DeleteMapping("")
  public ResponseEntity<ResponseDTO> delete(@RequestParam String id) {
    service.delete(UUID.fromString(id));
    ResponseDTO response = new ResponseDTO(200, null, "Producto eliminado con éxito");

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @DeleteMapping("/delete-by-account")
  public ResponseEntity<ResponseDTO> deleteByAccountId(@RequestParam String id) {
    service.deleteByAccountId(UUID.fromString(id));

    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    );
  }

  @DeleteMapping("/delete-by-ids")
  public ResponseEntity<ResponseDTO> deleteByIds(@RequestParam List<String> ids) {
    service.deleteByIds(ids.stream().map(id -> UUID.fromString(id)).toList());

    return new ResponseEntity<>(
      new ResponseDTO(200, null, null),
      HttpStatus.OK
    ); 
  }
}
