package com.nahuelgg.inventory_app.products.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.exceptions.EmptyFieldException;
import com.nahuelgg.inventory_app.products.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;
import com.nahuelgg.inventory_app.products.services.implementations.ProductService_Impl;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
  @Mock ProductRepository repository;

  @InjectMocks ProductService_Impl service;

  UUID acc1ID = UUID.randomUUID();
  UUID acc2ID = UUID.randomUUID();

  ProductEntity pr1, pr2, pr3;
  ProductDTO prDTO1, prDTO2, prDTO3;

  @BeforeEach
  void beforeEach() {
    pr1 = ProductEntity.builder()
      .id(UUID.randomUUID())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID)
    .build();
    pr2 = ProductEntity.builder()
      .id(UUID.randomUUID())
      .name("Ventilador de techo")
      .brand("Marca 2")
      .unitPrice(115.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID)
    .build();
    pr3 = ProductEntity.builder()
      .id(UUID.randomUUID())
      .name("Abrigo")
      .brand("Marca 3")
      .unitPrice(25.0)
      .categories(List.of("cat2"))
      .accountId(acc2ID)
    .build();

    prDTO1 = ProductDTO.builder()
      .id(pr1.getId().toString())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID.toString())
    .build();
    prDTO2 = ProductDTO.builder()
      .id(pr2.getId().toString())
      .name("Ventilador de techo")
      .brand("Marca 2")
      .unitPrice(115.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID.toString())
    .build();
    prDTO3 = ProductDTO.builder()
      .id(pr3.getId().toString())
      .name("Abrigo")
      .brand("Marca 3")
      .unitPrice(25.0)
      .categories(List.of("cat2"))
      .accountId(acc2ID.toString())
    .build();
  }

  @Test
  void search() {
    List<ProductDTO> expected = List.of(prDTO3);
    when(repository.search(null, null, null, null, acc2ID))
      .thenReturn(List.of(pr3));

    assertIterableEquals(expected, service.search(null, null, null, null, acc2ID));
  }

  @Test
  void search_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.search(null, null, null, null, null));
  }

  @Test
  void getByIds() {
    List<ProductDTO> expected = List.of(prDTO1, prDTO3);
    when(repository.findAllById(List.of(pr1.getId()))).thenReturn(List.of(pr1, pr3));

    assertIterableEquals(expected, service.getByIds(List.of(pr1.getId())));
  }

  @Test
  void getByIds_throwsEmptyField() {
    List<UUID> ids1 = new ArrayList<>();
    ids1.add(UUID.randomUUID());
    ids1.add(null);

    assertThrows(EmptyFieldException.class, 
      () -> service.getByIds(ids1));
    assertThrows(EmptyFieldException.class, 
      () -> service.getByIds(List.of()));
    assertThrows(EmptyFieldException.class, 
      () -> service.getByIds(null));
  }

  @Test
  void create() {
    when(repository.save(any(ProductEntity.class))).thenReturn(pr1);

    assertEquals(prDTO1, service.create(prDTO1));
  }

  @Test
  void create_throwsEmptyField() {
    ProductDTO withoutName = prDTO1.toBuilder().name("").build();
    ProductDTO withoutPrice = prDTO1.toBuilder().unitPrice(null).build();
    ProductDTO withoutBrand = prDTO1.toBuilder().brand("").build();
    ProductDTO withoutCat = prDTO1.toBuilder().categories(List.of()).build();
    
    assertThrows(EmptyFieldException.class, () -> service.create(null));
    assertThrows(EmptyFieldException.class, () -> service.create(withoutName));
    assertThrows(EmptyFieldException.class, () -> service.create(withoutPrice));
    assertThrows(EmptyFieldException.class, () -> service.create(withoutBrand));
    assertThrows(EmptyFieldException.class, () -> service.create(withoutCat));
  }

  @Test
  void update() {
    when(repository.findById(UUID.fromString(prDTO1.getId()))).thenReturn(Optional.of(pr1));
    when(repository.save(any(ProductEntity.class))).thenReturn(pr1);

    assertEquals(prDTO1, service.update(prDTO1));
  }

  @Test
  void update_throwsEmptyField() {
    ProductDTO invalid = prDTO1.toBuilder().id(null).build(); 
    
    assertThrows(EmptyFieldException.class, () -> service.update(null));
    assertThrows(EmptyFieldException.class, () -> service.update(invalid));
  }

  @Test
  void update_throwsNotFound() {
    String fakeId = UUID.randomUUID().toString();
    ProductDTO dto = ProductDTO.builder().id(fakeId).build();
    when(repository.findById(UUID.fromString(fakeId))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.update(dto));
  }

  @Test
  void delete() {
    UUID id = pr1.getId();
    when(repository.findById(id)).thenReturn(Optional.of(pr1));

    service.delete(id);

    verify(repository).deleteById(id);
  }

  @Test
  void delete_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.delete(null));
  }

  @Test
  void delete_throwsNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
  }

  @Test
  void deleteByAccountId() {
    UUID accId = UUID.randomUUID();
    List<ProductEntity> toDelete = List.of(pr1, pr2);
    when(repository.findByAccountId(accId)).thenReturn(toDelete);

    service.deleteByAccountId(accId);

    verify(repository).deleteAll(toDelete);
  }

  @Test
  void deleteByAccountId_throwsEmptyField() {
    assertThrows(EmptyFieldException.class, () -> service.deleteByAccountId(null));
  }

  @Test
  void deleteByIds() {
    List<UUID> ids = List.of(pr1.getId(), pr2.getId());

    service.deleteByIds(ids);

    verify(repository).deleteAllById(ids);
  }

  @Test
  void deleteByIds_throwsEmptyField() {
    List<UUID> ids1 = new ArrayList<>();
    ids1.add(UUID.randomUUID());
    ids1.add(null);

    assertThrows(EmptyFieldException.class, () -> service.deleteByIds(null));
    assertThrows(EmptyFieldException.class, () -> service.deleteByIds(List.of()));
    assertThrows(EmptyFieldException.class, () -> service.deleteByIds(ids1));
  } 
}
