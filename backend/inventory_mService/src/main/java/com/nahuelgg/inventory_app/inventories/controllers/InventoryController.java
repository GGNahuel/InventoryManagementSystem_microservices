package com.nahuelgg.inventory_app.inventories.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.EditProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
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
  public InventoryDTO getById(@Argument String id, @Argument String accountId) {
    if (!authorizationService.checkAccountIsLogged() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("Necesita iniciar sesión para realizar esta acción");
    return service.getById(UUID.fromString(id));
  }

  @QueryMapping
  public List<InventoryDTO> getByAccount(@Argument String accountId) {
    if (!authorizationService.checkAccountIsLogged() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("Necesita iniciar sesión para realizar esta acción");
    return service.getByAccount(UUID.fromString(accountId));
  }

  @QueryMapping
  public List<InventoryDTO> searchProductsInInventories(
    @Argument String name, @Argument String brand, @Argument String model, @Argument List<String> categories, @Argument String accountId
  ) {
    if (!authorizationService.checkAccountIsLogged() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("Necesita iniciar sesión para realizar esta acción");
    return service.searchProductsInInventories(name, brand, model, categories, UUID.fromString(accountId));
  }

  // Basic mutations
  @MutationMapping
  public InventoryDTO create(@Argument String name, @Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.create(name, UUID.fromString(accountId));
  }

  @MutationMapping
  public boolean edit(@Argument String invId, @Argument String name, @Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.edit(UUID.fromString(invId), name);
  }

  @MutationMapping
  public boolean delete(@Argument String id, @Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.delete(UUID.fromString(id), UUID.fromString(accountId));
  }

  @MutationMapping
  public boolean deleteByAccountId(@Argument String accountId) {
    if (!authorizationService.checkUserIsAdmin() || !authorizationService.checkActionIsToLoggedAccount(accountId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.deleteByAccountId(UUID.fromString(accountId));
  }

  // Product mutations
  @MutationMapping
  public ProductInInvDTO addProduct(@Argument ProductInputDTO product, @Argument String invId, @Argument String accountId) {
    if (!authorizationService.checkAccountIdAndUserPerm(accountId, Permissions.addProducts, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.addProduct(product, UUID.fromString(invId), UUID.fromString(accountId));
  }

  @MutationMapping
  public ProductInInvDTO editProductInInventory(@Argument EditProductInputDTO product, @Argument String invId, @Argument String accountId) {
    if (!authorizationService.checkAccountIdAndUserPerm(accountId, Permissions.editProducts, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.editProductInInventory(product, UUID.fromString(invId), UUID.fromString(accountId));
  }

  @MutationMapping
  public boolean copyProducts(@Argument List<ProductToCopyDTO> products, @Argument String idTo, @Argument String accountId) {
    if (!authorizationService.checkAccountIdAndUserPerm(accountId, Permissions.addProducts, idTo))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");
  
    return service.copyProducts(products, UUID.fromString(idTo));
  }

  @MutationMapping
  public boolean editStockOfProduct(@Argument int relativeNewStock, @Argument String productRefId, @Argument String invId, @Argument String accountId) {
    if (!authorizationService.checkAccountIdAndUserPerm(accountId, Permissions.editInventory, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.editStockOfProduct(relativeNewStock, UUID.fromString(productRefId), UUID.fromString(invId));
  }

  @MutationMapping
  public boolean deleteProductsInInventory(@Argument List<String> productRefIds, @Argument String invId, @Argument String accountId) {
    if (!authorizationService.checkAccountIdAndUserPerm(accountId, Permissions.deleteProducts, invId))
      throw new AccessDeniedException("No tiene permisos para realizar esta acción");

    return service.deleteProductInInventory(
      productRefIds.stream().map(string -> UUID.fromString(string)).toList(),
      UUID.fromString(invId), UUID.fromString(accountId)
    );
  }
}
