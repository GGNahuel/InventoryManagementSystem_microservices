package com.nahuelgg.inventory_app.products.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.enums.Permissions;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;
import com.nahuelgg.inventory_app.products.services.TokenGenerator;
import com.nahuelgg.inventory_app.products.utilities.Mappers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProductControllerTest {
  @Autowired TestRestTemplate restTemplate;
  @Autowired TokenGenerator tokenGenerator;
  @Autowired ProductRepository productRepository;
  
  Mappers mappers = new Mappers();
  
  @Value("${jwt_key}")
  String secretKey;
  String accUsername = "accUsername";
  UUID accId = UUID.randomUUID();
  String invId = UUID.randomUUID().toString();


  private HttpHeaders generateHeaderWithToken(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  @Test
  @DirtiesContext
  void getByIds() {
    ProductEntity pr1 = productRepository.save(ProductEntity.builder()
      .name("product1")
      .accountId(accId)
      .unitPrice(10.0)
    .build());
    ProductEntity pr2 = productRepository.save(ProductEntity.builder()
      .name("product2")
      .accountId(accId)
      .unitPrice(8.0)
    .build());
    UUID pr1Id = pr1.getId();
    UUID pr2Id = pr2.getId();

    String token = tokenGenerator.generateAccountToken(accUsername, accId.toString());

    String uri = UriComponentsBuilder.fromUriString("/product/ids")
      .queryParam("list", List.of(pr1Id.toString(), pr2Id.toString()).toArray())
    .toUriString();
    ResponseEntity<ResponseDTO<List<ProductDTO>>> response = restTemplate.exchange(
      uri, HttpMethod.GET, 
      new HttpEntity<>(generateHeaderWithToken(token)), 
      new ParameterizedTypeReference<ResponseDTO<List<ProductDTO>>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ResponseDTO<List<ProductDTO>> responseDTO = response.getBody();
    List<ProductDTO> actualData = responseDTO.getData();

    assertTrue(actualData.size() == 2);
    assertTrue(
      actualData.stream().anyMatch(pr -> pr.getId().equals(pr1Id.toString())) && 
      actualData.stream().anyMatch(pr -> pr.getId().equals(pr2Id.toString()))
    );
  }

  @Test
  @DirtiesContext
  void search() {
    ProductEntity pr1 = productRepository.save(ProductEntity.builder()
      .name("Ventilador de techo")
      .brand("marca 1")
      .model("ABC-123")
      .categories(List.of("electrodomésticos"))
      .accountId(accId)
      .unitPrice(0.0)
    .build());
    ProductEntity pr2 = productRepository.save(ProductEntity.builder()
      .name("Ventilador")
      .brand("marca 2")
      .model("45AB123")
      .categories(List.of("electrodomésticos"))
      .accountId(accId)
      .unitPrice(0.0)
    .build());
    ProductEntity pr3 = productRepository.save(ProductEntity.builder()
      .name("Estufa")
      .brand("marca 1")
      .model("DEF-123")
      .categories(List.of("electrodomésticos"))
      .accountId(accId)
      .unitPrice(0.0)
    .build());

    String token = tokenGenerator.generateAccountToken(accUsername, accId.toString());

    String uri = UriComponentsBuilder.fromUriString("/product/search")
      .queryParam("name", "Ventilador")
      .queryParam("brand", "")
      .queryParam("categoryNames", "electrodomésticos")
      .queryParam("accountId", accId.toString())
    .toUriString();

    ResponseEntity<ResponseDTO<List<ProductDTO>>> response = restTemplate.exchange(
      uri, HttpMethod.GET, 
      new HttpEntity<>(generateHeaderWithToken(token)), 
      new ParameterizedTypeReference<ResponseDTO<List<ProductDTO>>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ResponseDTO<List<ProductDTO>> responseDTO = response.getBody();
    List<ProductDTO> actualData = responseDTO.getData();

    assertTrue(actualData.size() == 2);
    assertTrue(
      actualData.stream().anyMatch(pr -> pr.getId().equals(pr1.getId().toString())) && 
      actualData.stream().anyMatch(pr -> pr.getId().equals(pr2.getId().toString()))
    );
    assertFalse(actualData.contains(mappers.mapEntityToDTO(pr3)));
  }

  @Test
  @DirtiesContext
  void create_successWithRightPerm() {
    ProductDTO input = ProductDTO.builder()
      .name("Celular")
      .brand("marca 1")
      .accountId(accId.toString())
      .unitPrice(1.0)
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.addProducts))
    .build()));
    
    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product?invId=" + invId + "&accountId=" + accId.toString(), HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode(), "El check de user con el perm ha fallado");

    ResponseDTO<ProductDTO> responseDTO = response.getBody();
    ProductDTO actual = responseDTO.getData();

    assertNotNull(actual);
    assertEquals("Celular", actual.getName());
    assertNotNull(actual.getId());
    assertTrue(productRepository.findById(UUID.fromString(actual.getId())).isPresent());
  }

  @Test
  @DirtiesContext
  void create_successIfAdmin() {
    ProductDTO input = ProductDTO.builder()
      .name("Celular")
      .brand("marca 1")
      .accountId(accId.toString())
      .unitPrice(1.0)
    .build();

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product?invId=" + invId + "&accountId=" + accId.toString(), HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode(), "El check de admin ha fallado");

    ResponseDTO<ProductDTO> responseDTO = response.getBody();
    ProductDTO actual = responseDTO.getData();

    assertNotNull(actual);
    assertEquals("Celular", actual.getName());
    assertNotNull(actual.getId());
    assertTrue(productRepository.findById(UUID.fromString(actual.getId())).isPresent());
  }

  @Test
  void create_denied() {
    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.editInventory))
    .build()));

    HttpEntity<ProductDTO> request = new HttpEntity<ProductDTO>(new ProductDTO(), generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      "/product?invId=" + invId + "&accountId=" + accId.toString(), HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    assertTrue(productRepository.findAll().isEmpty());
  }

  @Test
  @DirtiesContext
  void update_successWithRightPerm() {
    ProductEntity productToEdit = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(4.0)
    .build());

    ProductDTO input = ProductDTO.builder()
      .id(productToEdit.getId().toString())
      .name("product")
      .brand("marca")
      .unitPrice(5.2)
      .accountId(accId.toString())
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.editProductReferences))
    .build()));

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit?accountId=" + accId.toString(), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();
    ProductEntity productInDb = productRepository.findById(productToEdit.getId()).orElse(null);

    assertNotNull(productInDb);
    assertEquals(productToEdit.getId().toString(), actual.getId());
    assertTrue(productInDb.getBrand().equals("marca"));
    assertTrue(productInDb.getUnitPrice() == 5.2);
  }

  @Test
  @DirtiesContext
  void update_successIfAdmin() {
    ProductEntity productToEdit = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(4.0)
    .build());

    ProductDTO input = ProductDTO.builder()
      .id(productToEdit.getId().toString())
      .name("product")
      .brand("marca")
      .unitPrice(5.2)
      .accountId(accId.toString())
    .build();

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit?accountId=" + accId.toString(), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();
    ProductEntity productInDb = productRepository.findById(productToEdit.getId()).orElse(null);

    assertNotNull(productInDb);
    assertEquals(productToEdit.getId().toString(), actual.getId());
    assertTrue(productInDb.getBrand().equals("marca"));
    assertTrue(productInDb.getUnitPrice() == 5.2);
  }

  @Test
  void update_denied() {
    ProductDTO input = ProductDTO.builder()
      .id(UUID.randomUUID().toString())
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.editProducts))
    .build()));

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      "/product/edit?accountId=" + accId.toString(), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void updateInternal_successWithRightPerm() {
    ProductEntity productToEdit = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(4.0)
    .build());

    ProductDTO input = ProductDTO.builder()
      .id(productToEdit.getId().toString())
      .name("product")
      .brand("marca")
      .unitPrice(5.2)
      .accountId(accId.toString())
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(
      PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.editProducts))
      .build()
    ));

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit/common-perm?accountId=%s&invId=%s".formatted(accId.toString(), invId), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();
    ProductEntity productInDb = productRepository.findById(productToEdit.getId()).orElse(null);

    assertNotNull(productInDb);
    assertEquals(productToEdit.getId().toString(), actual.getId());
    assertTrue(productInDb.getBrand().equals("marca"));
    assertTrue(productInDb.getUnitPrice() == 5.2);
  }

  @Test
  @DirtiesContext
  void updateInternal_successIfAdmin() {
    ProductEntity productToEdit = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(4.0)
    .build());

    ProductDTO input = ProductDTO.builder()
      .id(productToEdit.getId().toString())
      .name("product")
      .brand("marca")
      .unitPrice(5.2)
      .accountId(accId.toString())
    .build();

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit/common-perm?accountId=%s&invId=%s".formatted(accId.toString(), invId), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();
    ProductEntity productInDb = productRepository.findById(productToEdit.getId()).orElse(null);

    assertNotNull(productInDb);
    assertEquals(productToEdit.getId().toString(), actual.getId());
    assertTrue(productInDb.getBrand().equals("marca"));
    assertTrue(productInDb.getUnitPrice() == 5.2);
  }

  @Test
  void updateInternal_deniedIfWrongPerm() {
    ProductDTO input = ProductDTO.builder()
      .id(UUID.randomUUID().toString())
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.editProductReferences))
    .build()));

    HttpEntity<ProductDTO> request = new HttpEntity<>(input, generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      "/product/edit/common-perm?accountId=%s&invId=%s".formatted(accId.toString(), invId), HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void delete_successWithRightPerm() {
    ProductEntity productToDelete = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(10.0)
    .build());

    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced(invId)
      .permissions(List.of(Permissions.deleteProductReferences))
    .build()));

    String uri = "/product/delete?id=" + productToDelete.getId().toString() + "&accountId=" + accId.toString();
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request, 
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());

    assertTrue(productRepository.findById(productToDelete.getId()).isEmpty());
  }

  @Test
  @DirtiesContext
  void delete_successIfAdmin() {
    ProductEntity productToDelete = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(10.0)
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    String uri = "/product/delete?id=" + productToDelete.getId().toString() + "&invId=" + invId + "&accountId=" + accId.toString();
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());

    assertTrue(productRepository.findById(productToDelete.getId()).isEmpty());
  }

  @Test
  void delete_denied() {
    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of(
      PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.deleteProducts))
      .build()
    ));

    String uri = "/product/delete?id=" + UUID.randomUUID().toString() + "&accountId=" + accId.toString();
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );

    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void deleteByAccountId_success() {
    ProductEntity pr1 = productRepository.save(ProductEntity.builder()
      .name("product1")
      .accountId(accId)
      .unitPrice(10.0)
      .categories(new ArrayList<>())
    .build());
    ProductEntity pr2 = productRepository.save(ProductEntity.builder()
      .name("product2")
      .accountId(UUID.randomUUID())
      .unitPrice(8.0)
      .categories(new ArrayList<>())
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    String uri = "/product/delete-by-account?accountId=" + accId.toString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(200, response.getBody().getStatus());

    assertTrue(productRepository.findById(pr1.getId()).isEmpty());
    assertTrue(productRepository.findAll().size() == 1);
    assertTrue(productRepository.findAll().get(0).getId().equals(pr2.getId()));
  }

  @Test
  void deleteByAccountId_denied() {
    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of());

    String uri = "/product/delete-by-account?accountId=" + accId.toString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(403, response.getBody().getStatus());
  }

  @Test
  @DirtiesContext
  void deleteByIds_success() {
    ProductEntity productToDelete1 = productRepository.save(ProductEntity.builder()
      .name("product")
      .accountId(accId)
      .unitPrice(10.0)
    .build());
    ProductEntity productToDelete2 = productRepository.save(ProductEntity.builder()
      .name("product2")
      .accountId(accId)
      .unitPrice(10.0)
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    String uri = UriComponentsBuilder.fromUriString("/product/delete-by-ids")
      .queryParam("ids", List.of(productToDelete1.getId(), productToDelete2.getId()))
      .queryParam("accountId", accId.toString())
    .toUriString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(200, response.getBody().getStatus());

    assertTrue(productRepository.findAll().isEmpty());
  }

  @Test
  void deleteByIds_denied() {
    String token = tokenGenerator.generateUserToken(accUsername, accId.toString(), List.of());

    String uri = UriComponentsBuilder.fromUriString("/product/delete-by-ids")
      .queryParam("ids", List.of())
      .queryParam("accountId", accId.toString())
    .toUriString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken(token));
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(403, response.getBody().getStatus());
  }

  @Test
  void deniedCasesWhenAccountIdParamIsNotOfTheLoggedOne() {
    ProductDTO input = ProductDTO.builder()
      .name("product")
      .unitPrice(10.0)
      .accountId(accId.toString())
    .build();

    String anotherAccId = UUID.randomUUID().toString();
    String adminToken = tokenGenerator.generateAdminToken(accUsername, accId.toString());

    HttpEntity<String> generalEntity = new HttpEntity<>(generateHeaderWithToken(adminToken));
    HttpEntity<ProductDTO> bodyEntity = new HttpEntity<ProductDTO>(input, generateHeaderWithToken(adminToken));

    ResponseEntity<ResponseDTO<Object>> createResponse = restTemplate.exchange(
      "/product?invId=" + invId + "&accountId=" + anotherAccId,
      HttpMethod.POST, bodyEntity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    ResponseEntity<ResponseDTO<Object>> updateResponse = restTemplate.exchange(
      "/product/edit?accountId=" + anotherAccId,
      HttpMethod.PUT, bodyEntity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    ResponseEntity<ResponseDTO<Object>> deleteResponse = restTemplate.exchange(
      "/product/delete?id=" + UUID.randomUUID().toString() + "&accountId=" + anotherAccId,
      HttpMethod.DELETE, generalEntity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    ResponseEntity<ResponseDTO<Object>> deleteByAccountResponse = restTemplate.exchange(
      "/product/delete-by-account?accountId=" + anotherAccId,
      HttpMethod.DELETE, generalEntity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    ResponseEntity<ResponseDTO<Object>> deleteByIdsResponse = restTemplate.exchange(
      UriComponentsBuilder.fromUriString("/product/delete-by-ids")
        .queryParam("ids", List.of(UUID.randomUUID().toString()))
        .queryParam("accountId", anotherAccId)
      .toUriString(),
      HttpMethod.DELETE, generalEntity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(HttpStatusCode.valueOf(403), createResponse.getStatusCode());
    assertEquals(HttpStatusCode.valueOf(403), updateResponse.getStatusCode());
    assertEquals(HttpStatusCode.valueOf(403), deleteResponse.getStatusCode());
    assertEquals(HttpStatusCode.valueOf(403), deleteByAccountResponse.getStatusCode());
    assertEquals(HttpStatusCode.valueOf(403), deleteByIdsResponse.getStatusCode());
  }
}
