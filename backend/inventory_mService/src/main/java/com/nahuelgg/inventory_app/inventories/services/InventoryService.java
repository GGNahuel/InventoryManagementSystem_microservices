package com.nahuelgg.inventory_app.inventories.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO;

public interface InventoryService {
  InventoryDTO getById(UUID id);
  List<InventoryDTO> getByAccount(UUID accountID);
  InventoryDTO getByNameAndAccount(String name, UUID accountId);
  List<InventoryDTO> searchProductsInInventories(
    String name, String brand, String model, List<String> categories, UUID accountId);
  InventoryDTO create(String name, UUID accountId);
  boolean edit(UUID id, String name);

  boolean addUser(UserFromUsersMSDTO user, UUID invId);
  ProductInInvDTO addProduct(ProductInputDTO product, UUID invID);
  boolean copyProducts(List<ProductToCopyDTO> products, UUID idTo);
  boolean editStockOfProduct(int relativeNewStock, UUID productRefId, UUID invId);
}
