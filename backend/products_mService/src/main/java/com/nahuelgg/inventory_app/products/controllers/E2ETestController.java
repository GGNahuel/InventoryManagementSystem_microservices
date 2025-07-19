package com.nahuelgg.inventory_app.products.controllers;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/e2e")
@Profile("e2e")
@RequiredArgsConstructor
public class E2ETestController {
  private final ProductRepository repository;

  @GetMapping("/products")
  public List<ProductEntity> getAll() {
    return repository.findAll();
  }
}
