package com.nahuelgg.inventory_app.products.services.implementations;

import static com.nahuelgg.inventory_app.products.utilities.Validations.checkFieldsHasContent;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;
import com.nahuelgg.inventory_app.products.services.ProductService;
import com.nahuelgg.inventory_app.products.utilities.Mappers;
import com.nahuelgg.inventory_app.products.utilities.Validations.Field;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService_Impl implements ProductService {
  private final ProductRepository repository;
  private final Mappers mappers = new Mappers();

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> search(String brand, String name, String model, List<String> categoryNames, UUID accountId) {
    checkFieldsHasContent(new Field("id de cuenta", accountId));
    
    categoryNames = categoryNames != null ? categoryNames.stream().map(ctgry -> URLDecoder.decode(ctgry, StandardCharsets.UTF_8).toLowerCase()).toList() : List.of();
    
    return repository.search(
      brand == null || brand.isBlank() ? null : brand.toLowerCase(),
      name == null || name.isBlank() ? null : name.toLowerCase(),
      model == null || model.isBlank() ? null : model.toLowerCase(),
      categoryNames == null || categoryNames.isEmpty() ? null : categoryNames.stream().map(ctgy -> ctgy.toLowerCase()).toList(),
      accountId
    ).stream().map(p -> mappers.mapEntityToDTO(p)).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<ProductDTO> getByIds(List<UUID> ids) {
    checkFieldsHasContent(new Field("lista de Id", ids));

    return repository.findAllById(ids).stream().map(p -> mappers.mapEntityToDTO(p)).toList();
  }

  @Override @Transactional
  public ProductDTO create(ProductDTO productToCreate) {
    checkFieldsHasContent(new Field("producto a crear", productToCreate));
    checkFieldsHasContent(
      new Field("nombre del producto", productToCreate.getName()),
      new Field("precio unitario", productToCreate.getUnitPrice()),
      new Field("marca", productToCreate.getBrand()),
      new Field("cuenta asociada", productToCreate.getAccountId())
    );

    return mappers.mapEntityToDTO(repository.save(mappers.mapDTOToEntity(productToCreate)));
  }

  @Override @Transactional
  public ProductDTO update(ProductDTO updatedProduct) {
    checkFieldsHasContent(new Field("producto actualizado", updatedProduct));
    checkFieldsHasContent(new Field("id del original", updatedProduct.getId()));
    repository.findById(UUID.fromString(updatedProduct.getId())).orElseThrow(
      () -> new ResourceNotFoundException("producto", "id", updatedProduct.getId().toString())
    );

    return mappers.mapEntityToDTO(repository.save(mappers.mapDTOToEntity(updatedProduct)));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id));
    repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("producto", "id", id.toString())
    );

    repository.deleteById(id);
  }

  @Override @Transactional
  public void deleteByAccountId(UUID id) {
    checkFieldsHasContent(new Field("id", id));
    List<ProductEntity> productsToDelete = repository.findByAccountId(id);
    repository.deleteAll(productsToDelete);
  }

  @Override @Transactional
  public void deleteByIds(List<UUID> ids) {
    checkFieldsHasContent(new Field("ids", ids));
    repository.deleteAllById(ids);    
  }
}
