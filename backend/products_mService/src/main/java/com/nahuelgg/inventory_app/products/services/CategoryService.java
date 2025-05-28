package com.nahuelgg.inventory_app.products.services;

import java.util.List;
import java.util.UUID;

import com.nahuelgg.inventory_app.products.dtos.CategoryDTO;

public interface CategoryService {
  public List<CategoryDTO> getAll();
  public CategoryDTO getByName(String name);
  public CategoryDTO getById(UUID id);
  public CategoryDTO create(String name);
  public void update(CategoryDTO categoryToUpdate);
  public void delete(UUID id);
}
