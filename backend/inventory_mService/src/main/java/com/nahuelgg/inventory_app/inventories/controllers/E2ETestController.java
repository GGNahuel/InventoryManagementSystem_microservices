package com.nahuelgg.inventory_app.inventories.controllers;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.repositories.UserReferenceRepository;
import com.nahuelgg.inventory_app.inventories.utilities.Mappers;

import lombok.RequiredArgsConstructor;

@Controller
@Profile("e2e")
@RequiredArgsConstructor
public class E2ETestController {
  private final InventoryRepository inventoryRepository;
  private final ProductInInvRepository pInInvRepository;
  private final UserReferenceRepository userReferenceRepository;
  
  private final Mappers mapper = new Mappers();

  @QueryMapping
  public List<InventoryDTO> getAllInventories() {
    return inventoryRepository.findAll().stream().map(
      inv -> mapper.mapInvEntity(inv, List.of())
    ).toList();
  }

  @QueryMapping
  public List<ProductInInvEntity> getAllProductsInInv() {
    return pInInvRepository.findAll();
  }

  @QueryMapping List<UserReferenceEntity> getAllUserReferences() {
    return userReferenceRepository.findAll();
  }
}
