package com.nahuelgg.inventory_app.inventories.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.GraphQlTester.Response;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.repositories.UserReferenceRepository;
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
  @Autowired UserReferenceRepository userReferenceRepository;
  @Autowired ProductInInvRepository productInInvRepository;

  @MockitoBean RestTemplate restCaller;
  
  HttpGraphQlTester graphQlTester;

  @LocalServerPort
  int port;

  String accId = UUID.randomUUID().toString();
  String accUsername = "accUsername";
  InventoryDTO inv;

  private Consumer<HttpHeaders> generateHeaderWithToken(String token) {
    return headers -> headers.setBearerAuth(token);
  }

  @BeforeEach
  void setUp() {
    graphQlTester = HttpGraphQlTester.create(webClientBuilder);
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
      .usersIds(new ArrayList<>())
      .products(new ArrayList<>())
    .build();

    when(restCaller.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(new ResponseEntity<>(
      ResponseDTO.builder().data(List.of()).build(), HttpStatus.OK
    ));

    String token = tokenGenerator.generateAccountToken(accUsername, accId);

    String query = """
      query {
        getById(id: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }    
    """.formatted(invToSearch.getId().toString());

    Response response = graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute();
    response.errors().satisfy(errors -> errors.isEmpty());
    response.path("getById").entity(InventoryDTO.class).isEqualTo(expected);
  }

  @Test
  void getById_deniedIfUnauthenticated() {
    UUID idToSearch = UUID.randomUUID();

    String query = """
      query {
        getById(id: "%s") {
          id
          name
          accountId
          usersIds
          products {
            name
          }
        }
      }
    """.formatted(idToSearch);

    Response response = graphQlTester.mutate().url("http://localhost:" + port + "/graphql").build()
      .document(query).execute();

    response.errors().satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Unauthorized"));
    });
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

    Response response = graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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

    Response response = graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
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
        edit(invId: "%s", name: "newName")
      }
    """.formatted(savedInv.getId().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("edit").entity(Boolean.class).isEqualTo(true);

    assertTrue(inventoryRepository.findById(savedInv.getId()).get().getName().equals("newName"));
  }

  @Test
  void edit_denyIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        edit(invId: "%s", name: "newName")
      }
    """.formatted(UUID.randomUUID().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
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
        delete(id: "%s")
      }
    """.formatted(invToDelete.getId().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("delete").entity(Boolean.class).isEqualTo(true);

    assertTrue(inventoryRepository.findById(invToDelete.getId()).isEmpty());
  }

  @Test
  void delete_deniedIfNotAdmin() {
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        delete(id: "%s")
      }
    """.formatted(UUID.randomUUID());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
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

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }

  // Users related endpoints
  @Test
  void addUser_allowedIfUserIsAdmin() {
    UUID refUserId = UUID.randomUUID();
    InventoryEntity savedInv = inventoryRepository.save(InventoryEntity.builder()
      .name("inv")
      .accountId(UUID.fromString(accId))
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    String query = """
      mutation {
        addUser(userId: "%s", invId: "%s")
      }
    """.formatted(refUserId.toString(), savedInv.getId().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("addUser").entity(Boolean.class).isEqualTo(true);

    InventoryEntity invWithChanges = inventoryRepository.findById(savedInv.getId()).get();
    assertFalse(invWithChanges.getUsers().isEmpty());
    assertEquals(refUserId, invWithChanges.getUsers().get(0).getReferenceId());
  }

  @Test
  void addUser_denyIfNotAdmin() {
    UUID userId = UUID.randomUUID();
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        addUser(userId: "%s", invId: "%s")
      }
    """.formatted(userId.toString(), UUID.randomUUID().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }

  @Test
  void removeUser_allowedIfUserIsAdmin() {
    UUID refUserId = UUID.randomUUID();
    UserReferenceEntity userReferenceToRemove = userReferenceRepository.save(UserReferenceEntity.builder()
      .referenceId(refUserId)
    .build());
    InventoryEntity savedInvWithUser1 = inventoryRepository.save(InventoryEntity.builder()
      .name("inv1")
      .accountId(UUID.fromString(accId))
      .users(new ArrayList<>(List.of(userReferenceToRemove)))
    .build());
    InventoryEntity savedInvWithUser2 = inventoryRepository.save(InventoryEntity.builder()
      .name("inv2")
      .accountId(UUID.fromString(accId))
      .users(new ArrayList<>(List.of(userReferenceToRemove)))
    .build());

    String token = tokenGenerator.generateAdminToken(accUsername, accId);

    String query = """
      mutation {
        removeUser(userId: "%s", accountId: "%s")
      }
    """.formatted(refUserId.toString(), accId.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().path("removeUser").entity(Boolean.class).isEqualTo(true);

    InventoryEntity inv1WithChanges = inventoryRepository.findById(savedInvWithUser1.getId()).get();
    InventoryEntity inv2WithChanges = inventoryRepository.findById(savedInvWithUser2.getId()).get();
    assertTrue(inv1WithChanges.getUsers().stream().noneMatch(userRefEntity -> userRefEntity.getReferenceId().equals(refUserId)));
    assertTrue(inv2WithChanges.getUsers().stream().noneMatch(userRefEntity -> userRefEntity.getReferenceId().equals(refUserId)));
  }

  @Test
  void remove_denyIfNotAdmin() {
    UUID refUserId = UUID.randomUUID();
    String token = tokenGenerator.generateUserToken(accUsername, accId, null);

    String query = """
      mutation {
        removeUser(userId: "%s", accountId: "%s")
      }
    """.formatted(refUserId.toString(), accId.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }

  // Products related endpoints
  @Test
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
      "invId", savedInv.getId().toString()
    );

    String query = """
      mutation($product: ProductInput!, $invId: ID!) {
        addProduct(product: $product, invId: $invId) {
          refId
          name
          brand
          unitPrice
          stock
        }
      }
    """;

    Response response = graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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
      "invId", destinyInvId.toString()
    );

    String query = """
      mutation($product: ProductInput!, $invId: ID!) {
        addProduct(product: $product, invId: $invId) {
          name
          brand
          unitPrice
          stock
        }
      }
    """;

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
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
        copyProducts(products: $products, idTo: $idTo)
      }
    """;

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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
        copyProducts(products: $products, idTo: $idTo)
      }
    """;

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).variables(variables).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
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
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s")
      }
    """.formatted(refId.toString(), savedInv.getId().toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
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
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s")
      }
    """.formatted(refId.toString(), invId.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken(token)).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.size() == 1);
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }
}
