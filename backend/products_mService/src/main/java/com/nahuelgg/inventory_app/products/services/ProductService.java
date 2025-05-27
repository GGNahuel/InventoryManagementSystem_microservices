package com.nahuelgg.inventory_app.products.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.products.DTOs.ProductDTO;

public interface ProductService {
  public List<ProductDTO> getAll();
  public List<ProductDTO> getByCategoryName(String category);
  public List<ProductDTO> searchByBrandNameAndModel(String brand, String name, String model);
  public List<ProductDTO> getByIds(List<UUID> ids);
  public ProductDTO create(ProductDTO productToCreate);
  public ProductDTO update(ProductDTO updatedProduct);
  public void delete(UUID id);
}
