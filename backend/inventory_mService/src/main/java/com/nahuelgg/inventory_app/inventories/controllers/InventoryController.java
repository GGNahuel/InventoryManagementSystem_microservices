package com.nahuelgg.inventory_app.inventories.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;

@Controller
public class InventoryController {
  private final InventoryService service;

  public InventoryController(InventoryService service) {
    this.service = service;
  }

  @QueryMapping
  public InventoryDTO getById(@Argument String id) {
    return service.getById(UUID.fromString(id));
  }

  @QueryMapping
  public List<InventoryDTO> getByAccountId(@Argument String accountId) {
    return service.getByAccount(UUID.fromString(accountId));
  }

  @QueryMapping
  public InventoryDTO getByNameAndAccount(@Argument String invName, @Argument String accountID) {
    return service.getByNameAndAccount(invName, UUID.fromString(accountID));
  }

  @QueryMapping
  public List<InventoryDTO> searchProductsInInventories(
    @Argument String name, @Argument String brand, @Argument String model, @Argument List<String> categories, @Argument String accountId
  ) {
    return service.searchProductsInInventories(name, brand, model, categories, UUID.fromString(name));
  }

  @MutationMapping
  public InventoryDTO create(@Argument String name, @Argument String accountId) {
    return service.create(name, UUID.fromString(accountId));
  }

  @MutationMapping
  public String edit(@Argument String invId, @Argument String name) {
    if (service.edit(UUID.fromString(invId), name))
      return "Nombre editado con éxito";
    else return "No se pudo editar";
  }

  @MutationMapping
  public boolean delete(@Argument String id) {
    return service.delete(UUID.fromString(id));
  }

  @MutationMapping
  public boolean deleteByAccountId(@Argument String id) {
    return service.deleteByAccountId(UUID.fromString(id));
  }

  @MutationMapping
  public boolean addUser(@Argument UserFromUsersMSDTO user, @Argument String invId) {
    return service.addUser(user, UUID.fromString(invId));
  }

  @MutationMapping
  public boolean removeUser(@Argument String userId, @Argument String accountId) {
    return service.removeUser(UUID.fromString(userId), UUID.fromString(accountId));
  }

  @MutationMapping
  public ProductInInvDTO addProduct(@Argument ProductInputDTO product, @Argument String invId) {
    return service.addProduct(product, UUID.fromString(invId));
  }

  @MutationMapping
  public boolean copyProducts(@Argument List<ProductToCopyDTO> products, @Argument String idTo) {
    return service.copyProducts(products, UUID.fromString(idTo));
  }

  @MutationMapping
  public boolean editStockOfProduct(@Argument int relativeNewStock, @Argument String productRefId, @Argument String invId) {
    return service.editStockOfProduct(relativeNewStock, UUID.fromString(productRefId), UUID.fromString(invId));
  }
}
