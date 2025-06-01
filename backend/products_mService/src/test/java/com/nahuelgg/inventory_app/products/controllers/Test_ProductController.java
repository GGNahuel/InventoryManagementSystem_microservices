package com.nahuelgg.inventory_app.products.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.entities.CategoryEntity;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.services.ProductService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test_ProductController {
  @Autowired TestRestTemplate restTemplate;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean ProductService service;

  UUID acc1ID = UUID.randomUUID();
  CategoryEntity c1 = CategoryEntity.builder().id(UUID.randomUUID()).name("catA").build();
  ProductEntity pr1;
  ProductDTO prDTO1;

  @BeforeEach
  void beforeEach() {
    pr1 = ProductEntity.builder()
      .id(UUID.randomUUID())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of(c1))
      .accountId(acc1ID)
    .build();

    prDTO1 = ProductDTO.builder()
      .id(pr1.getId().toString())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of(c1.getName()))
      .accountId(acc1ID.toString())
    .build();
  }

  @Test
  void getByIds() {
    when(service.getByIds(anyList())).thenReturn(List.of(prDTO1));
    String uri = UriComponentsBuilder.fromUriString("/product/ids")
      .queryParam("list", List.of(prDTO1.getId()).toArray())
    .toUriString();
    ResponseDTO response = restTemplate.getForObject(uri, ResponseDTO.class);
    List<ProductDTO> actualData = objectMapper
      .convertValue(response.getData(), new TypeReference<List<ProductDTO>>() {});

    assertEquals(200, response.getStatus());
    assertNull(response.getError());
    assertNotNull(response.getData());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void search() {
    when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of(prDTO1));

    String uri = UriComponentsBuilder.fromUriString("/product/search")
      .queryParam("brand", "Marca 1")
      .queryParam("name", "Ventilador")
      .queryParam("categoryNames", "catA")
      .queryParam("accountId", acc1ID.toString())
      .toUriString();

    ResponseDTO response = restTemplate.getForObject(uri, ResponseDTO.class);
    List<ProductDTO> actualData = objectMapper.convertValue(response.getData(), new TypeReference<List<ProductDTO>>() {});

    assertEquals(200, response.getStatus());
    assertNull(response.getError());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void create() {
    when(service.create(any())).thenReturn(prDTO1);

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1);
    ResponseDTO response = restTemplate.postForObject("/product", request, ResponseDTO.class);
    ProductDTO actual = objectMapper.convertValue(response.getData(), ProductDTO.class);

    assertEquals(201, response.getStatus());
    assertNull(response.getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void update() {
    when(service.update(any())).thenReturn(prDTO1);

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1);
    ResponseEntity<ResponseDTO> response = restTemplate.exchange("/product", HttpMethod.PUT, request, ResponseDTO.class);
    ProductDTO actual = objectMapper.convertValue(response.getBody().getData(), ProductDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void deleteById() {
    String uri = "/product?id=" + prDTO1.getId();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertEquals("Producto eliminado con éxito", response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByAccountId() {
    String uri = "/product/delete_by_account?id=" + acc1ID;

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByIds() {
    String uri = UriComponentsBuilder.fromUriString("/product/delete_by_ids")
      .queryParam("ids", prDTO1.getId())
      .toUriString();

    ResponseEntity<ResponseDTO> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseDTO.class);

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }
}
