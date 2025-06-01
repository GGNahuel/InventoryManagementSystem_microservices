package com.nahuelgg.inventory_app.products.services.implementations;

import static com.nahuelgg.inventory_app.products.utilities.Validations.*;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.products.repositories.CategoryRepository;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;
import com.nahuelgg.inventory_app.products.services.ProductService;
import com.nahuelgg.inventory_app.products.utilities.Validations.Field;

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
      .accountId(p.getAccountId().toString())
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
          () -> new ResourceNotFoundException("categoría", "nombre", name)
        )
      ).toList())
      .accountId(UUID.fromString(p.getAccountId()))
    .build();
  }

  // CRUD IMPLEMENTATION METHODS
  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getAll() {
    return repository.findAll().stream().map(p -> mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> search(String brand, String name, String model, List<String> categoryNames, UUID accountId) {
    return repository.search(
      brand == null || brand.isBlank() ? null : brand,
      name == null || name.isBlank() ? null : name,
      model == null || model.isBlank() ? null : model,
      categoryNames == null || categoryNames.isEmpty() ? null : categoryNames,
      accountId
    ).stream().map(p -> mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getByIds(List<UUID> ids) {
    checkFieldsHasContent(new Field("lista de Id", ids));

    return ids.stream().map(
      id -> mapEntityToDTO(repository.findById(id).orElseThrow(
        () -> new ResourceNotFoundException("producto", "id", id.toString())
      ))
    ).toList();
  }

  @Override @Transactional
  public ProductDTO create(ProductDTO productToCreate) {
    checkFieldsHasContent(new Field("producto a crear", productToCreate));
    checkFieldsHasContent(
      new Field("nombre de producto", productToCreate.getName()),
      new Field("precio unitario", productToCreate.getUnitPrice()),
      new Field("marca", productToCreate.getBrand()),
      new Field("lista de categorías", productToCreate.getCategories())
    );

    return mapEntityToDTO(repository.save(mapDTOToEntity(productToCreate)));
  }

  @Override @Transactional
  public ProductDTO update(ProductDTO updatedProduct) {
    checkFieldsHasContent(new Field("producto actualizado", updatedProduct));
    checkFieldsHasContent(new Field("id del original", updatedProduct.getId()));
    repository.findById(UUID.fromString(updatedProduct.getId())).orElseThrow(
      () -> new ResourceNotFoundException("producto", "id", updatedProduct.getId().toString())
    );

    return mapEntityToDTO(repository.save(mapDTOToEntity(updatedProduct)));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id));
    repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("producto", "id", id.toString())
    );

    repository.deleteById(id);
  }
}
