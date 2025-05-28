package com.nahuelgg.inventory_app.products.services.implementations;

import static com.nahuelgg.inventory_app.products.utilities.Validations.*;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.products.dtos.CategoryDTO;
import com.nahuelgg.inventory_app.products.entities.CategoryEntity;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
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
    checkFieldsHasContent(new Field("nombre", name));

    return mapEntityToDTO(repository.findByName(name).orElseThrow(
      () -> new ResourceNotFoundException("categoría", "nombre", name)
    ));
  }

  @Override @Transactional(readOnly = true)
  public CategoryDTO getById(UUID id) {
    checkFieldsHasContent(new Field("id", id.toString()));

    return mapEntityToDTO(repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("categoría", "id", id.toString())
    ));
  }

  @Override @Transactional
  public CategoryDTO create(String name) {
    checkFieldsHasContent(new Field("nombre", name));

    return mapEntityToDTO(repository.save(CategoryEntity.builder().name(name).build()));
  }

  @Override @Transactional
  public void update(CategoryDTO categoryToUpdate) {
    checkFieldsHasContent(new Field("categoría a actualizar", categoryToUpdate));
    checkFieldsHasContent(new Field("id", categoryToUpdate.getId()));
    repository.findById(UUID.fromString(categoryToUpdate.getId())).orElseThrow(
      () -> new ResourceNotFoundException("categoría", "id", categoryToUpdate.getId().toString())
    );

    repository.save(mapDTOToEntity(categoryToUpdate));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id.toString()));
    repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("categoría", "id", id.toString())
    );

    repository.deleteById(id);
  }
}
