package com.nahuelgg.inventory_app.inventories.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.EditProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;

public interface InventoryService {
  InventoryDTO getById(UUID id);
  List<InventoryDTO> getByAccount(UUID accountID);
  List<InventoryDTO> searchProductsInInventories(
    String name, String brand, String model, List<String> categories, UUID accountId
  );

  InventoryDTO create(String name, UUID accountId);
  boolean edit(UUID id, String name);
  boolean delete(UUID id, UUID accountId);
  boolean deleteByAccountId(UUID id);

  ProductInInvDTO addProduct(ProductInputDTO product, UUID invID, UUID accountId);
  ProductInInvDTO editProductInInventory(EditProductInputDTO product, UUID invId, UUID accountId);
  boolean copyProducts(List<ProductToCopyDTO> products, UUID idTo);
  boolean editStockOfProduct(int relativeNewStock, UUID productRefId, UUID invId);
  boolean deleteProductInInventory(List<UUID> productRefIds, UUID invId, UUID accountId);
}
