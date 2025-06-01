package com.nahuelgg.inventory_app.inventories.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;

@Controller
public class InventoryController {
  private final InventoryService service;

  public InventoryController(InventoryService service) {
    this.service = service;
  }

  @QueryMapping
  public InventoryDTO getById(String id) {
    return service.getById(UUID.fromString(id));
  }

  @QueryMapping
  public List<InventoryDTO> getByAccountId(String accountId) {
    return service.getByAccount(UUID.fromString(accountId));
  }

  @QueryMapping
  public InventoryDTO getByNameAndAccount(String invName, String accountID) {
    return service.getByNameAndAccount(invName, UUID.fromString(accountID));
  }

  @QueryMapping
  public List<InventoryDTO> searchProductsInInventories(
    String name, String brand, String model, List<String> categories, String accountId
  ) {
    return service.searchProductsInInventories(name, brand, model, categories, UUID.fromString(name));
  }

  @MutationMapping
  public InventoryDTO create(String name, String accountId) {
    return service.create(name, UUID.fromString(accountId));
  }

  @MutationMapping
  public String edit(String invId, String name) {
    if (service.edit(UUID.fromString(invId), name))
      return "Nombre editado con éxito";
    else return "No se pudo editar";
  }

  @MutationMapping
  public boolean delete(String id) {
    return service.delete(UUID.fromString(id));
  }

  @MutationMapping
  public boolean addUser(UserFromUsersMSDTO user, String invId) {
    return service.addUser(user, UUID.fromString(invId));
  }
}
