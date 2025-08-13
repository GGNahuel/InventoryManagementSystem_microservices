package com.nahuelgg.inventory_app.products.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;

  @GetMapping("/ids")
  public ResponseEntity<ResponseDTO<List<ProductDTO>>> getByIds(@RequestParam List<String> list) {
    List<UUID> uuidList = list.stream().map(string -> UUID.fromString(string)).toList();
    ResponseDTO<List<ProductDTO>> response = new ResponseDTO<>(200, null, service.getByIds(uuidList));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/search")
  public ResponseEntity<ResponseDTO<List<ProductDTO>>> search(
    @RequestParam(required = false) String brand,
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String model,
    @RequestParam(required = false) List<String> categoryNames,
    @RequestParam String accountId
  ) {
    ResponseDTO<List<ProductDTO>> response = new ResponseDTO<>(200, null, service.search(brand, name, model, categoryNames, UUID.fromString(accountId)));

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("")
  @PreAuthorize("@authorizationService.checkUserHasPerm('addProducts', #invId) && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<ProductDTO>> create(@RequestBody ProductDTO product, @RequestParam String invId, @RequestParam String accountId) {
    ResponseDTO<ProductDTO> response = new ResponseDTO<>(201, null, service.create(product));
    
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
  
  @PutMapping("/edit")
  @PreAuthorize("@authorizationService.checkUserHasPerm('editProductReferences', '') && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<ProductDTO>> update(@RequestBody ProductDTO product, @RequestParam String accountId) {
    ResponseDTO<ProductDTO> response = new ResponseDTO<>(200, null, service.update(product));
    
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PutMapping("/edit/common-perm")
  @PreAuthorize("@authorizationService.checkUserHasPerm('editProducts', #invId) && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<ProductDTO>> updateInternal(@RequestBody ProductDTO product, @RequestParam String invId, @RequestParam String accountId) {
    ResponseDTO<ProductDTO> response = new ResponseDTO<>(200, null, service.update(product));
    
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
  
  @DeleteMapping("/delete")
  @PreAuthorize("@authorizationService.checkUserHasPerm('deleteProductReferences', '') && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<String>> delete(@RequestParam String id, @RequestParam String accountId) {
    service.delete(UUID.fromString(id));

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @DeleteMapping("/delete-by-account")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<Object>> deleteByAccountId(@RequestParam String accountId) {
    service.deleteByAccountId(UUID.fromString(accountId));

    return new ResponseEntity<>(
      new ResponseDTO<>(200, null, null),
      HttpStatus.OK
    );
  }

  @DeleteMapping("/delete-by-ids")
  @PreAuthorize("@authorizationService.checkUserIsAdmin() && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<Object>> deleteByIds(@RequestParam List<String> ids, @RequestParam String accountId) {
    service.deleteByIds(ids.stream().map(id -> UUID.fromString(id)).toList());

    return new ResponseEntity<>(
      new ResponseDTO<>(200, null, null),
      HttpStatus.OK
    ); 
  }

  @DeleteMapping("/delete-by-ids/common-perm")
  @PreAuthorize("@authorizationService.checkUserHasPerm('deleteProducts', #invId) && @authorizationService.checkActionIsToLoggedAccount(#accountId)")
  public ResponseEntity<ResponseDTO<String>> deleteInternal(@RequestParam List<String> ids, @RequestParam String invId, @RequestParam String accountId) {
    service.deleteByIds(ids.stream().map(id -> UUID.fromString(id)).toList());

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
