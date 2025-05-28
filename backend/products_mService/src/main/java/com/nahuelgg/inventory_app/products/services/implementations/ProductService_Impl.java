package com.nahuelgg.inventory_app.products.services.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.repositories.CategoryRepository;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;
import com.nahuelgg.inventory_app.products.services.ProductService;

@Service
public class ProductService_Impl implements ProductService {
  private final ProductRepository repository;
  private final CategoryRepository categoryRepository;

  public ProductService_Impl(ProductRepository repository, CategoryRepository categoryRepository) {
    this.repository = repository;
    this.categoryRepository = categoryRepository;
  };

  // MAPPERS
  private ProductDTO mapEntityToDTO(ProductEntity p) {
    return ProductDTO.builder()
      .id(p.getId().toString())
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories().stream().map(
        category -> category.getName()
      ).toList())
    .build();
  }

  private ProductEntity mapDTOToEntity(ProductDTO p) {
    return ProductEntity.builder()
      .id(UUID.fromString(p.getId()))
      .name(p.getName())
      .brand(p.getBrand())
      .model(p.getModel())
      .description(p.getDescription())
      .unitPrice(p.getUnitPrice())
      .categories(p.getCategories().stream().map(
        name -> categoryRepository.findByName(name).orElseThrow(
          () -> new RuntimeException("No se pudo asociar la categoría con el nombre " + name)
        )
      ).toList())
    .build();
  }

  // CRUD IMPLEMENTATION METHODS
  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getAll() {
    return repository.findAll().stream().map(p -> mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getByCategoryName(String category) {
    return repository.findByCategoryName(category).stream().map(p -> mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> searchByBrandNameAndModel(String brand, String name, String model) {
    return repository.findByBrandNameAndModel(
      brand.isBlank() ? null : brand,
      name.isBlank() ? null : name,
      model.isBlank() ? null : model
    ).stream().map(p -> mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getByIds(List<UUID> ids) {
    return ids.stream().map(
      id -> mapEntityToDTO(repository.findById(id).orElseThrow(
        () -> new RuntimeException("No se encontró el producto con el id: " + id.toString())
      ))
    ).toList();
  }

  @Override @Transactional
  public ProductDTO create(ProductDTO productToCreate) {
    return mapEntityToDTO(repository.save(mapDTOToEntity(productToCreate)));
  }

  @Override @Transactional
  public ProductDTO update(ProductDTO updatedProduct) {
    repository.findById(UUID.fromString(updatedProduct.getId())).orElseThrow(
      () -> new RuntimeException("No se puede actualizar debido a que no se encontró el producto con el id " + updatedProduct.getId())
    );

    return mapEntityToDTO(repository.save(mapDTOToEntity(updatedProduct)));
  }

  @Override @Transactional
  public void delete(UUID id) {
    repository.findById(id).orElseThrow(
      () -> new RuntimeException("No se puede eliminar debido a que no se encontró el producto con el id " + id)
    );

    repository.deleteById(id);
  }
}
