package com.nahuelgg.inventory_app.products.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;

public interface ProductService {
  public List<ProductDTO> getAll();
  public List<ProductDTO> search(String brand, String name, String model, List<String> categoryNames, UUID accountId);
  public List<ProductDTO> getByIds(List<UUID> ids);
  public ProductDTO create(ProductDTO productToCreate);
  public ProductDTO update(ProductDTO updatedProduct);
  public void delete(UUID id);
  public void deleteByAccountId(UUID id);
  public void deleteByIds(List<UUID> ids);
}
