package com.nahuelgg.inventory_app.inventories.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.GraphQlTester.Response;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.AccountFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.UserFromUsersMSDTO.InventoryPermsDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.EditProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.services.JwtService;
import com.nahuelgg.inventory_app.inventories.services.TokenGenerator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ControllerTest {
  @Autowired WebTestClient webClientBuilder;
  @Autowired ObjectMapper objectMapper;
  @Autowired JwtService jwtService;
  @Autowired TokenGenerator tokenGenerator;

  @Autowired InventoryRepository inventoryRepository;
  @Autowired ProductInInvRepository productInInvRepository;

  @MockitoBean RestTemplate restCaller;
  
  HttpGraphQlTester graphQlTester;

  @LocalServerPort
  int port;

  String accId = UUID.randomUUID().toString();
  String accUsername = "accUsername";
  String url;

  private Consumer<HttpHeaders> generateHeaderWithToken(String token) {
    return headers -> headers.setBearerAuth(token);
  }

  private void checkOperationIsForbidden(String query, String token, Map<String, Object> variables) {
    HttpGraphQlTester tester = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build();
    Response response = variables == null ? tester.document(query).execute() :  tester.document(query).variables(variables).execute();

   response.errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }

  private void checkOperationIsUnauthorized(String query, Map<String, Object> variables) {
    HttpGraphQlTester tester = graphQlTester.mutate().url(url).build();
    Response response = variables == null ? tester.document(query).execute() :  tester.document(query).variables(variables).execute();
    System.out.println(response.returnResponse().toString());
    response.errors().satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Unauthorized"));
    });
  }

  @BeforeEach
  void setUp() {
    graphQlTester = HttpGraphQlTester.create(webClientBuilder);

    url = "http://localhost:" + port + "/graphql";
  }

  // Inventory crud tests
  @Test
  void getById_success() {
    InventoryEntity invToSearch = inventoryRepository.save(InventoryEntity.builder()
      .name("inventario1")
      .accountId(UUID.fromString(accId))
    .build());
    InventoryDTO expected = InventoryDTO.builder()
      .id(invToSearch.getId().toString())
      .name(invToSearch.getName())
      .accountId(accId)
      .products(new ArrayList<>())
    .build();

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(List.of()).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAccountToken(accUsername, accId);

    String query = """
      query {
        getById(id: "%s", accountId: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }    
    """.formatted(invToSearch.getId().toString(), accId);

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute();
    response.errors().satisfy(errors -> errors.isEmpty());
    response.path("getById").entity(InventoryDTO.class).isEqualTo(expected);
  }

  @Test
  void getById_deniedIfUnauthenticated() {
    UUID idToSearch = UUID.randomUUID();

    String query = """
      query {
        getById(id: "%s", accountId: "%s") {
          id
          name
          accountId
          products {
            name
          }
        }
      }
    """.formatted(idToSearch, accId);

    checkOperationIsUnauthorized(query, null);
  }

  @Test
  void getByAccount_allowed() {
    InventoryEntity inv1 = inventoryRepository.save(InventoryEntity.builder()
      .name("inv1")
      .accountId(UUID.fromString(accId))
    .build());
    InventoryEntity inv2 = inventoryRepository.save(InventoryEntity.builder()
      .name("inv2")
      .accountId(UUID.fromString(accId))
    .build());
    InventoryEntity inv3 = inventoryRepository.save(InventoryEntity.builder()
      .name("invOfAnotherAccount")
      .accountId(UUID.randomUUID())
    .build());

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(List.of()).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAccountToken(accUsername, accId);

    String query = """
      query {
        getByAccount(accountId: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }    
    """.formatted(accId);

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute();
    response.errors().satisfy(errors -> errors.isEmpty());

    List<InventoryDTO> result = response.path("getByAccount").entityList(InventoryDTO.class).get();

    assertTrue(result.stream().noneMatch(invDto -> invDto.getId().equals(inv3.getId().toString())));
    assertTrue(
      result.stream().filter(invDto -> invDto.getName().equals(inv1.getName())).findFirst().isPresent() &&
      result.stream().filter(invDto -> invDto.getName().equals(inv2.getName())).findFirst().isPresent() 
    );
  }

  @Test
  void create_allowIfUserIsAdmin() {
    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder()
        .data(AccountFromUsersMSDTO.builder().id(accId).build())
      .build(), HttpStatus.OK
    ));

    String query = """
      mutation {
        create(name: "inventoryA", accountId: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }
    """.formatted(accId.toString());

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute();
    response.errors().satisfy(errors -> errors.isEmpty());

    InventoryDTO result = response.path("create").entity(InventoryDTO.class).get();
    assertEquals("inventoryA", result.getName());
    assertEquals(accId, result.getAccountId());
    assertTrue(inventoryRepository.findById(UUID.fromString(result.getId())).isPresent());
  }

  @Test
  void create_denyIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        create(name: "inventoryA", accountId: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }
    """.formatted(accId.toString());

    checkOperationIsForbidden(query, token, null);
  }

  @Test
  void edit_allowIfUserIsAdmin() {
    InventoryEntity savedInv = inventoryRepository.save(InventoryEntity.builder()
      .name("previousName")
      .accountId(UUID.fromString(accId))
    .build());
    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    String query = """
      mutation {
        edit(invId: "%s", name: "newName", accountId: "%s")
      }
    """.formatted(savedInv.getId().toString(), accId);

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("edit").entity(Boolean.class).isEqualTo(true);

    assertTrue(inventoryRepository.findById(savedInv.getId()).get().getName().equals("newName"));
  }

  @Test
  void edit_denyIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        edit(invId: "%s", name: "newName", accountId: "%s")
      }
    """.formatted(UUID.randomUUID().toString(), accId);

    checkOperationIsForbidden(query, token, null);
  }

  @Test
  void delete_allowedIfUserIsAdmin() {
    InventoryEntity invToDelete = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      new ResponseDTO(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    String query = """
      mutation {
        delete(id: "%s", accountId: "%s")
      }
    """.formatted(invToDelete.getId().toString(), accId);

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("delete").entity(Boolean.class).isEqualTo(true);

    assertTrue(inventoryRepository.findById(invToDelete.getId()).isEmpty());
  }

  @Test
  void delete_deniedIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        delete(id: "%s", accountId: "%s")
      }
    """.formatted(UUID.randomUUID(), accId);

    checkOperationIsForbidden(query, token, null);
  }

  @Test
  void deleteByAccountId_allowedIfUserIsAdmin() {
    inventoryRepository.save(InventoryEntity.builder()
      .name("inv1")
      .accountId(UUID.fromString(accId))
    .build());
    inventoryRepository.save(InventoryEntity.builder()
      .name("inv2")
      .accountId(UUID.fromString(accId))
    .build());
    inventoryRepository.save(InventoryEntity.builder()
      .name("invOfAnotherAccount")
      .accountId(UUID.randomUUID())
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    String query = """
      mutation {
        deleteByAccountId(accountId: "%s")
      }
    """.formatted(accId.toString());

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("deleteByAccountId").entity(Boolean.class).isEqualTo(true);

    List<InventoryEntity> invsInDB = inventoryRepository.findByAccountId(UUID.fromString(accId));
    assertEquals(0, invsInDB.size());
    assertTrue(invsInDB.stream().noneMatch(inv -> inv.getAccountId().equals(UUID.fromString(accId))));
  }

  @Test
  void deleteByAccountId_deniedIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        deleteByAccountId(accountId: "%s")
      }
    """.formatted(accId.toString());

    checkOperationIsForbidden(query, token, null);
  }

  // Products related endpoints
  @Test
  @DirtiesContext
  void search_successAndHasOnlyExpectedProducts() {
    UUID ref1 = UUID.randomUUID();
    UUID ref2 = UUID.randomUUID();
    UUID ref3 = UUID.randomUUID();

    // preparar base de datos
    InventoryEntity invSaved = inventoryRepository.save(InventoryEntity.builder()
      .name("inv1")
      .accountId(UUID.fromString(accId))
    .build());
    InventoryEntity invSaved2 = inventoryRepository.save(InventoryEntity.builder()
      .name("inv2")
      .accountId(UUID.fromString(accId))
    .build());

      // productos en el inventario 1
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref1).stock(3).inventory(invSaved)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref2).stock(5).inventory(invSaved)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref3).stock(7).inventory(invSaved)
    .build());

      // productos en el inventario 2
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref1).stock(4).inventory(invSaved2)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref3).stock(6).inventory(invSaved2)
    .build());

    // datos en la BDD del servicio de productos
    ProductFromProductsMSDTO prMs1 = ProductFromProductsMSDTO.builder()
      .id(ref1.toString()).name("product").accountId(accId)
    .build();
    ProductFromProductsMSDTO prMs2 = ProductFromProductsMSDTO.builder()
      .id(ref2.toString()).name("product").accountId(accId)
    .build();

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(List.of(prMs1, prMs2)).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAccountToken(accUsername, accId);
    String query = """
      query {
        searchProductsInInventories(name: "pr", accountId: "%s") {
          name
          products {
            name
            refId
          }
        }
      }    
    """.formatted(accId);

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute();

    List<InventoryDTO> result = response.path("searchProductsInInventories").entityList(InventoryDTO.class).get();

    assertEquals(2, result.size());
    assertEquals(2, result.get(0).getProducts().size());
    assertEquals(1, result.get(1).getProducts().size());
    assertTrue(
      result.get(0).getProducts().stream().noneMatch(pr -> pr.getRefId().equals(ref3.toString())) &&
      result.get(1).getProducts().stream().noneMatch(pr -> pr.getRefId().equals(ref3.toString())) 
    );
  }

  @Test
  void search_deniedIfNotAuthenticated() {
    String query = """
      query {
        searchProductsInInventories(name: "pr", accountId: "%s") {
          name
          products {
            name
            refId
          }
        }
      }    
    """.formatted(accId);

    checkOperationIsUnauthorized(query, null);
  }

  @Test
  @DirtiesContext
  void addProduct_allowIfHasRightPerm() {
    InventoryEntity savedInv = inventoryRepository.save(InventoryEntity.builder()
      .name(accUsername)
      .accountId(UUID.fromString(accId))
    .build());

    UUID productId = UUID.randomUUID();
    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder()
        .data(ProductFromProductsMSDTO.builder()
          .id(productId.toString())
          .name("product")
          .brand("brand")
          .unitPrice(2)
          .accountId(accId)
        .build())
      .build(),
      HttpStatus.OK
    ));

    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(savedInv.getId().toString())
      .permissions(List.of(Permissions.addProducts))
    .build()));

    Map<String, Object> variables = Map.of(
      "product", Map.of(
        "name", "product",
        "brand", "brand",
        "unitPrice", 2,
        "stock", 4
      ),
      "invId", savedInv.getId().toString(),
      "accountId", accId
    );

    String query = """
      mutation($product: ProductInput!, $invId: ID!, $accountId: ID!) {
        addProduct(product: $product, invId: $invId, accountId: $accountId) {
          refId
          name
          brand
          unitPrice
          stock
        }
      }
    """;

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute();

    ProductInInvDTO result = response.path("addProduct").entity(ProductInInvDTO.class).get();

    assertTrue(
      result.getName().equals("product") &&
      result.getRefId().equals(productId.toString()) &&
      productInInvRepository.findByReferenceIdAndInventoryId(productId, savedInv.getId()).isPresent()
    );
  }

  @Test
  void addProduct_denyIfHasWrongPerm() {
    UUID destinyInvId = UUID.randomUUID();
    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(destinyInvId.toString())
      .permissions(List.of(Permissions.editInventory))
    .build()));

    Map<String, Object> variables = Map.of(
      "product", Map.of(
        "name", "product",
        "brand", "brand",
        "unitPrice", 2,
        "stock", 4
      ),
      "invId", destinyInvId.toString(),
      "accountId", accId
    );

    String query = """
      mutation($product: ProductInput!, $invId: ID!, $accountId: ID!) {
        addProduct(product: $product, invId: $invId, accountId: $accountId) {
          name
          brand
          unitPrice
          stock
        }
      }
    """;

    checkOperationIsForbidden(query, token, variables);
  }

  @Test
  @DirtiesContext
  void editProductInInventory_successIfHasRightPerm_UniqueReference() {
    UUID refId = UUID.randomUUID();

    InventoryEntity inv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(refId).stock(4).inventory(inv)
    .build());

    EditProductInputDTO input = EditProductInputDTO.builder()
      .refId(refId.toString()).name("name2")
    .build();

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(ProductFromProductsMSDTO.builder()
        .id(refId.toString())
        .name("name2")
      .build()).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId().toString()).permissions(List.of(Permissions.editProducts))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "product", input,
      "invId", inv.getId().toString(),
      "accountId", accId
    );
    String query = """
      mutation($product: EditProductInput!, $invId: ID!, $accountId: ID!) {
        editProductInInventory(product: $product, invId: $invId, accountId: $accountId) {
          refId
          name
        }
      }
    """;

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute();

    ProductInInvDTO result = response.path("editProductInInventory").entity(ProductInInvDTO.class).get();
    assertEquals(refId.toString(), result.getRefId());

    ArgumentCaptor<String> capture = ArgumentCaptor.forClass(String.class);
    verify(restCaller).exchange(capture.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());
    assertEquals(
      "http://api-products:8081/product/edit/common-perm?invId=%s&accountId=%s".formatted(inv.getId().toString(), accId),
      capture.getValue()
    );
  }

  @Test
  @DirtiesContext
  void editProductInInventory_successIfAdmin_SharedReference() {
    UUID refId = UUID.randomUUID();

    InventoryEntity inv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());
    InventoryEntity anotherInv = inventoryRepository.save(InventoryEntity.builder()
      .name("anotherInv").accountId(UUID.fromString(accId))
    .build());

    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(refId).stock(4).inventory(inv)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(refId).stock(6).inventory(anotherInv)
    .build());

    EditProductInputDTO input = EditProductInputDTO.builder()
      .refId(refId.toString()).name("name2")
    .build();

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(ProductFromProductsMSDTO.builder()
        .id(refId.toString())
        .name("name2")
      .build()).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    Map<String, Object> variables = Map.of(
      "product", input,
      "invId", inv.getId().toString(),
      "accountId", accId
    );
    String query = """
      mutation($product: EditProductInput!, $invId: ID!, $accountId: ID!) {
        editProductInInventory(product: $product, invId: $invId, accountId: $accountId) {
          refId
          name
        }
      }
    """;

    Response response = graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute();

    ProductInInvDTO result = response.path("editProductInInventory").entity(ProductInInvDTO.class).get();
    assertEquals(refId.toString(), result.getRefId());

    ArgumentCaptor<String> capture = ArgumentCaptor.forClass(String.class);
    verify(restCaller).exchange(capture.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());
    assertEquals(
      "http://api-products:8081/product?invId=%s&accountId=%s".formatted(inv.getId().toString(), accId),
      capture.getValue()
    );
  }

  @Test
  @DirtiesContext
  void editProductInInventory_deniedIfWrongPerm() {
    InventoryEntity inv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());

    EditProductInputDTO input = EditProductInputDTO.builder()
      .refId(UUID.randomUUID().toString()).name("name2")
    .build();

    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId().toString()).permissions(List.of(Permissions.editProductReferences))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "product", input,
      "invId", inv.getId().toString(),
      "accountId", accId
    );
    String query = """
      mutation($product: EditProductInput!, $invId: ID!, $accountId: ID!) {
        editProductInInventory(product: $product, invId: $invId, accountId: $accountId) {
          refId
          name
        }
      }
    """;

    checkOperationIsForbidden(query, token, variables);
  }

  @Test
  @DirtiesContext
  void deleteProductInInventory_allowIfHasRightPerm_checkMakeRightCall() {
    UUID ref1 = UUID.randomUUID();
    UUID ref2 = UUID.randomUUID();
    UUID ref3 = UUID.randomUUID();

    InventoryEntity inv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());
    
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref1).inventory(inv)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref2).inventory(inv)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref3).inventory(inv)
    .build());

    InventoryEntity anotherInv = inventoryRepository.save(InventoryEntity.builder()
      .name("anotherInv").accountId(UUID.fromString(accId))
    .build());
    // en la url al servicio de productos no se debería incluir la id de ref 3 ya que está también en un producto en otro inventario
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref2).inventory(anotherInv)
    .build());

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(inv.getId().toString()).permissions(List.of(Permissions.deleteProducts))
    .build()));

    Map<String, Object> variables = Map.of(
      "idList", List.of(ref1, ref2, ref3).stream().map(uuid -> uuid.toString()).toList(),
      "invId", inv.getId().toString(),
      "accountId", accId
    );
    String query = """
      mutation($idList: [ID]!, $invId: ID!, $accountId: ID!) {
        deleteProductsInInventory(productRefIds: $idList, invId: $invId, accountId: $accountId)
      }
    """;

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build().document(query).variables(variables).execute();

    assertEquals(0, productInInvRepository.findByInventory(inv).size());
    assertEquals(1, productInInvRepository.findByReferenceId(ref2).size());
    
    ArgumentCaptor<String> capturer = ArgumentCaptor.forClass(String.class);
    verify(restCaller).exchange(capturer.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());
    assertTrue(capturer.getValue().contains("http://api-products:8081/product/delete-by-ids/common-perm"));
    assertTrue(capturer.getValue().contains(ref1.toString()));
    assertFalse(capturer.getValue().contains(ref2.toString()));
    assertTrue(capturer.getValue().contains(ref3.toString()));
  }
//TODO: ser consistente con los nombres de los permisos en todos los servicios
  @Test
  @DirtiesContext
  void deleteProductInInventory_deniedIfWrongPerm() {
    UUID ref1 = UUID.randomUUID();
    UUID ref2 = UUID.randomUUID();
    UUID ref3 = UUID.randomUUID();

    InventoryEntity inv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv").accountId(UUID.fromString(accId))
    .build());
    
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref1).inventory(inv)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref2).inventory(inv)
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref3).inventory(inv)
    .build());

    InventoryEntity anotherInv = inventoryRepository.save(InventoryEntity.builder()
      .name("anotherInv").accountId(UUID.fromString(accId))
    .build());
    productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(ref2).inventory(anotherInv)
    .build());

    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(inv.getId().toString()).permissions(List.of(Permissions.deleteProductReferences))
    .build()));

    Map<String, Object> variables = Map.of(
      "idList", List.of(ref1, ref2, ref3).stream().map(uuid -> uuid.toString()).toList(),
      "invId", inv.getId().toString(),
      "accountId", accId
    );
    String query = """
      mutation($idList: [ID]!, $invId: ID!, $accountId: ID!) {
        deleteProductsInInventory(productRefIds: $idList, invId: $invId, accountId: $accountId)
      }
    """;

    checkOperationIsForbidden(query, token, variables);

    assertEquals(3, productInInvRepository.findByInventory(inv).size());
    assertEquals(2, productInInvRepository.findByReferenceId(ref2).size());
  }

  @Test
  void copyProducts_allowedIfHasRightPerm() throws JsonProcessingException {
    InventoryEntity savedInv = inventoryRepository.save(InventoryEntity.builder()
      .name("destinyInv")
      .accountId(UUID.fromString(accId))
    .build());

    UUID refId1 = UUID.randomUUID();
    UUID refId2 = UUID.randomUUID();
    List<ProductToCopyDTO> productsToCopyDTO = List.of(
      new ProductToCopyDTO(refId1.toString(), 4),
      new ProductToCopyDTO(refId2.toString(), 6)
    );
    
    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(savedInv.getId().toString())
      .permissions(List.of(Permissions.addProducts))
    .build()));

    Map<String, Object> variables = Map.of(
      "products", productsToCopyDTO,
      "idTo", savedInv.getId().toString()
    );

    String query = """
      mutation($products: [ProductToCopyInput]!, $idTo: ID!) {
        copyProducts(products: $products, idTo: $idTo, accountId: "%s")
      }
    """.formatted(accId);

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute().path("copyProducts").entity(Boolean.class).isEqualTo(true);

    assertTrue(
      productInInvRepository.findByReferenceIdAndInventoryId(refId1, savedInv.getId()).isPresent() &&
      productInInvRepository.findByReferenceIdAndInventoryId(refId2, savedInv.getId()).isPresent()
    );
  }

  @Test
  void copyProducts_deniedIfHasWrongPerm() {
    UUID existingInvId = UUID.randomUUID();
    ProductToCopyDTO input = ProductToCopyDTO.builder()
      .refId(UUID.randomUUID().toString())
      .stock(4)
    .build();
    
    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(existingInvId.toString())
      .permissions(List.of(Permissions.editProducts))
    .build()));

    Map<String, Object> variables = Map.of(
      "products", List.of(Map.of(
        "refId", input.getRefId(),
        "stock", input.getStock()
      )),
      "idTo", existingInvId.toString()
    );

    String query = """
      mutation($products: [ProductToCopyInput]!, $idTo: ID!) {
        copyProducts(products: $products, idTo: $idTo, accountId: "%s")
      }
    """.formatted(accId);

    checkOperationIsForbidden(query, token, variables);
  }

  // TODO: test for every method, should deny if no user account is logged

  @Test
  void editStockOfProduct_allowIfHasRightPerm() {
    InventoryEntity savedInv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv")
      .accountId(UUID.fromString(accId))
    .build());

    UUID refId = UUID.randomUUID();
    ProductInInvEntity savedProductInInv = productInInvRepository.save(ProductInInvEntity.builder()
      .referenceId(refId)
      .stock(4)
      .isAvailable(true)
      .inventory(savedInv)
    .build());
    
    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(savedInv.getId().toString())
      .permissions(List.of(Permissions.editInventory))
    .build()));

    String query = """
      mutation {
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s", accountId: "%s")
      }
    """.formatted(refId.toString(), savedInv.getId().toString(), accId);

    graphQlTester.mutate().url(url).headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("editStockOfProduct").entity(Boolean.class).isEqualTo(true);

    assertTrue(productInInvRepository.findById(savedProductInInv.getId()).get().getStock() == 6);
  }

  @Test
  void editStockOfProduct_deniedIfHasWrongPerm() {
    UUID invId = UUID.randomUUID();
    UUID refId = UUID.randomUUID();
    String token = tokenGenerator.generateUserToken(accUsername, accId, List.of(InventoryPermsDTO.builder()
      .idOfInventoryReferenced(invId.toString())
      .permissions(List.of(Permissions.addProducts))
    .build()));

    String query = """
      mutation {
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s", accountId: "%s")
      }
    """.formatted(refId.toString(), invId.toString(), accId);

    checkOperationIsForbidden(query, token, null);
  }
}
