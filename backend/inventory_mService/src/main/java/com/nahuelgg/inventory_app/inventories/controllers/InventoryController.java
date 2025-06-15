package com.nahuelgg.inventory_app.inventories.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.services.AuthorizationService;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InventoryController {
  private final InventoryService service;
  private final AuthorizationService authorizationService;

  // Queries
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

  // Basic mutations
  @MutationMapping
  public InventoryDTO create(@Argument String name, @Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.create(name, UUID.fromString(accountId));
  }

  @MutationMapping
  public String edit(@Argument String invId, @Argument String name) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    if (service.edit(UUID.fromString(invId), name))
      return "Nombre editado con éxito";
    else return "No se pudo editar";
  }

  @MutationMapping
  public boolean delete(@Argument String id) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.delete(UUID.fromString(id));
  }

  @MutationMapping
  public boolean deleteByAccountId(@Argument String id) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.deleteByAccountId(UUID.fromString(id));
  }

  // User mutations
  @MutationMapping
  public boolean addUser(@Argument UserFromUsersMSDTO user, @Argument String invId) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.addUser(user, UUID.fromString(invId));
  }

  @MutationMapping
  public boolean removeUser(@Argument String userId, @Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin())
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.removeUser(UUID.fromString(userId), UUID.fromString(accountId));
  }

  // Product mutations
  @MutationMapping
  public ProductInInvDTO addProduct(@Argument ProductInputDTO product, @Argument String invId) {
    if (!authorizationService.checkUserHasPerm(Permissions.addProducts, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.addProduct(product, UUID.fromString(invId));
  }

  @MutationMapping
  public boolean copyProducts(@Argument List<ProductToCopyDTO> products, @Argument String idTo) {
    if (!authorizationService.checkUserHasPerm(Permissions.addProducts, idTo))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.copyProducts(products, UUID.fromString(idTo));
  }

  @MutationMapping
  public boolean editStockOfProduct(@Argument int relativeNewStock, @Argument String productRefId, @Argument String invId) {
    if (!authorizationService.checkUserHasPerm(Permissions.editInventory, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.editStockOfProduct(relativeNewStock, UUID.fromString(productRefId), UUID.fromString(invId));
  }
}
