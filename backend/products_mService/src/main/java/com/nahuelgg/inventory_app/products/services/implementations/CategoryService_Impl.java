package com.nahuelgg.inventory_app.products.services.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.products.dtos.CategoryDTO;
import com.nahuelgg.inventory_app.products.entities.CategoryEntity;
import com.nahuelgg.inventory_app.products.repositories.CategoryRepository;
import com.nahuelgg.inventory_app.products.services.CategoryService;

@Service
public class CategoryService_Impl implements CategoryService {
  private final CategoryRepository repository;

  public CategoryService_Impl(CategoryRepository repository) {
    this.repository = repository;
  }

  // MAPPERS
  private CategoryDTO mapEntityToDTO(CategoryEntity c) {
    return CategoryDTO.builder()
      .id(c.getId().toString())
      .name(c.getName())
    .build();
  }

  private CategoryEntity mapDTOToEntity(CategoryDTO c) {
    return CategoryEntity.builder()
      .id(UUID.fromString(c.getId()))
      .name(c.getName())
    .build();
  }

  // CRUD IMPLEMENTATION METHODS
  @Override @Transactional(readOnly = true)
  public List<CategoryDTO> getAll() {
    return repository.findAll().stream().map(c -> mapEntityToDTO(c)).toList();
  }

  @Override @Transactional(readOnly = true)
  public CategoryDTO getByName(String name) {
    return mapEntityToDTO(repository.findByName(name).orElseThrow(
      () -> new RuntimeException("No se encontró ninguna categoría con el nombre " + name)
    ));
  }

  @Override @Transactional(readOnly = true)
  public CategoryDTO getById(UUID id) {
    return mapEntityToDTO(repository.findById(id).orElseThrow(
      () -> new RuntimeException("No se encontró ninguna categoría con la id " + id)
    ));
  }

  @Override @Transactional
  public CategoryDTO create(String name) {
    return mapEntityToDTO(repository.save(CategoryEntity.builder().name(name).build()));
  }

  @Override @Transactional
  public void update(CategoryDTO categoryToUpdate) {
    repository.findById(UUID.fromString(categoryToUpdate.getId())).orElseThrow(
      () -> new RuntimeException("No se puede actualizar debido a que no se encontró una categoría con la id dada")
    );

    repository.save(mapDTOToEntity(categoryToUpdate));
  }

  @Override @Transactional
  public void delete(UUID id) {
    repository.findById(id).orElseThrow(
      () -> new RuntimeException("No se puede actualizar debido a que no se encontró una categoría con la id dada")
    );

    repository.deleteById(id);
  }
}
