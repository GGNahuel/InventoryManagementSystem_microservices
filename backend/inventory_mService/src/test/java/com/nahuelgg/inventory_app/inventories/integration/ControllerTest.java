package com.nahuelgg.inventory_app.inventories.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO.InventoryPermsDTO;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;
import com.nahuelgg.inventory_app.inventories.services.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ControllerTest {
  @Autowired WebTestClient webClientBuilder;
  @Autowired ObjectMapper objectMapper;
  
  HttpGraphQlTester graphQlTester;

  @LocalServerPort
  int port;

  @MockitoBean InventoryService inventoryService;
  @MockitoBean JwtService jwtService;

  UUID accID = UUID.randomUUID();
  String accUsername = "username";
  String token = "testToken";
  InventoryDTO inv = InventoryDTO.builder()
    .id(UUID.randomUUID().toString())
    .name("inventoryA")
    .accountId(accID.toString())
    .usersIds(new ArrayList<>())
  .build();

  private void configJwtMock(String userName, String userRole, boolean isAdmin, List<InventoryPermsDTO> userPerms) {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject(accUsername);
      claims.put("accountId", accID.toString());
      claims.put("userName", userName);
      claims.put("userRole", userRole);
      claims.put("isAdmin", isAdmin);
      claims.put("userPerms", userPerms);
      return claimGetter.apply(claims);
    });
    when(jwtService.isTokenExpired(token)).thenReturn(false);
  }

  private Consumer<HttpHeaders> generateHeaderWithToken() {
    return headers -> headers.setBearerAuth(token);
  }

  @BeforeEach
  void setUp() {
    graphQlTester = HttpGraphQlTester.create(webClientBuilder);
  }

  @Test
  void getById_allowedIfAuthenticated() {
    when(inventoryService.getById(UUID.fromString(inv.getId()))).thenReturn(inv);
    configJwtMock(null, null, false, null);

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
    """.formatted(inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("getById").entity(InventoryDTO.class).isEqualTo(inv);

    verify(inventoryService).getById(UUID.fromString(inv.getId()));
  }

  @Test
  void getById_failsIfUnauthenticated() {
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
    """.formatted(idToSearch.toString());

    webClientBuilder.post()
      .uri("/graphql")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"query\":\"" + query.replace("\"", "\"") + "\"}")
      .exchange()
    .expectStatus().isForbidden();
  }

  @Test
  void getByAccount_allowed() {
    when(inventoryService.getByAccount(accID)).thenReturn(List.of(inv));
    configJwtMock(null, null, false, null);

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
    """.formatted(accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("getByAccount[0]").entity(InventoryDTO.class).isEqualTo(inv);

    verify(inventoryService).getByAccount(accID);
  }

  @Test
  void create_allowIfUserIsAdmin() {
    when(inventoryService.create("inventoryA", accID)).thenReturn(inv);
    configJwtMock("userAdmin", "role", true, null);

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
    """.formatted(accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("create").entity(InventoryDTO.class).isEqualTo(inv);

    verify(inventoryService).create("inventoryA", accID);
  }

  @Test
  void create_denyIfNotAdmin() {
    configJwtMock("user", "role", false, null);

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
    """.formatted(accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).create(anyString(), any());
  }

  @Test
  void edit_allowIfUserIsAdmin() {
    when(inventoryService.edit(UUID.fromString(inv.getId()), "inventoryA")).thenReturn(true);
    when(inventoryService.edit(UUID.fromString(inv.getId()), "inventoryB")).thenReturn(false);
    configJwtMock("userAdmin", "role", true, null);

    String query1 = """
      mutation {
        edit(invId: "%s", name: "inventoryA")
      }
    """.formatted(inv.getId());
    String query2 = """
      mutation {
        edit(invId: "%s", name: "inventoryB")
      }
    """.formatted(inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query1).execute().path("edit").entity(Boolean.class).isEqualTo(true);

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query2).execute().path("edit").entity(Boolean.class).isEqualTo(false);

    verify(inventoryService).edit(UUID.fromString(inv.getId()), "inventoryA");
    verify(inventoryService).edit(UUID.fromString(inv.getId()), "inventoryB");
  }

  @Test
  void edit_denyIfNotAdmin() {
    configJwtMock("user", "role", false, null);

    String query = """
      mutation {
        edit(invId: "%s", name: "inventoryA")
      }
    """.formatted(inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).edit(any(), any());
  }

  @Test
  void delete_allowedIfUserIsAdmin() {
    when(inventoryService.delete(UUID.fromString(inv.getId()))).thenReturn(true);
    configJwtMock("userAdmin", "role", true, null);

    String query = """
      mutation {
        delete(id: "%s")
      }
    """.formatted(inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("delete").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).delete(UUID.fromString(inv.getId()));
  }

  @Test
  void delete_deniedIfNotAdmin() {
    configJwtMock("user", "role", false, null);

    String query = """
      mutation {
        delete(id: "%s")
      }
    """.formatted(inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).delete(any());
  }

  @Test
  void deleteByAccountId_allowedIfUserIsAdmin() {
    when(inventoryService.deleteByAccountId(accID)).thenReturn(true);
    configJwtMock("userAdmin", "role", true, null);

    String query = """
      mutation {
        deleteByAccountId(id: "%s")
      }
    """.formatted(accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("deleteByAccountId").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).deleteByAccountId(accID);
  }

  @Test
  void deleteByAccountId_deniedIfNotAdmin() {
    configJwtMock("user", "role", false, null);

    String query = """
      mutation {
        deleteByAccountId(id: "%s")
      }
    """.formatted(accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).delete(any());
  }

  @Test
  void addUser_allowedIfUserIsAdmin() {
    UUID userId = UUID.randomUUID();
    when(inventoryService.addUser(userId, UUID.fromString(inv.getId()))).thenReturn(true);
    configJwtMock("userAdmin", "role", true, null);

    String query = """
      mutation {
        addUser(userId: "%s", invId: "%s")
      }
    """.formatted(userId.toString(), inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("addUser").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).addUser(userId, UUID.fromString(inv.getId()));
  }

  @Test
  void addUser_denyIfNotAdmin() {
    UUID userId = UUID.randomUUID();
    configJwtMock("user", "role", false, null);

    String query = """
      mutation {
        addUser(userId: "%s", invId: "%s")
      }
    """.formatted(userId.toString(), inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).addUser(any(), any());
  }

  @Test
  void removeUser_allowedIfUserIsAdmin() {
    UUID userId = UUID.randomUUID();
    when(inventoryService.removeUser(userId, accID)).thenReturn(true);
    configJwtMock("userAdmin", "role", true, null);

    String query = """
      mutation {
        removeUser(userId: "%s", accountId: "%s")
      }
    """.formatted(userId.toString(), accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("removeUser").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).removeUser(userId, accID);
  }

  @Test
  void remove_denyIfNotAdmin() {
    UUID userId = UUID.randomUUID();
    configJwtMock("user", "role", false, null);

    String query = """
      mutation {
        removeUser(userId: "%s", accountId: "%s")
      }
    """.formatted(userId.toString(), accID.toString());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).removeUser(any(), any());
  }

  @Test
  void addProduct_allowIfHasRightPerm() {
    ProductInputDTO input = ProductInputDTO.builder()
      .name("product")
      .brand("brand")
      .unitPrice(2)
      .stock(4)
    .build();
    ProductInInvDTO expected = ProductInInvDTO.builder()
      .name("product")
      .brand("brand")
      .unitPrice(2)
      .stock(4)
    .build();
    
    when(inventoryService.addProduct(input, UUID.fromString(inv.getId()))).thenReturn(expected);
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.addProducts))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "product", Map.of(
        "name", "product",
        "brand", "brand",
        "unitPrice", 2,
        "stock", 4
      ),
      "invId", inv.getId()
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

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).variables(variables).execute().path("addProduct").entity(ProductInInvDTO.class).isEqualTo(expected);
  }

  @Test
  void addProduct_denyIfHasWrongPerm() {
    ProductInputDTO input = ProductInputDTO.builder()
      .name("product")
      .brand("brand")
      .unitPrice(2)
      .stock(4)
    .build();
    ProductInInvDTO expected = ProductInInvDTO.builder()
      .name("product")
      .brand("brand")
      .unitPrice(2)
      .stock(4)
    .build();
    
    when(inventoryService.addProduct(input, UUID.fromString(inv.getId()))).thenReturn(expected);
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.editInventory))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "product", Map.of(
        "name", "product",
        "brand", "brand",
        "unitPrice", 2,
        "stock", 4
      ),
      "invId", inv.getId()
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

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).variables(variables).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });
  }

  @Test
  void copyProducts_allowedIfHasRightPerm() {
    ProductToCopyDTO input = ProductToCopyDTO.builder()
      .refId(UUID.randomUUID().toString())
      .stock(4)
    .build();
    
    when(inventoryService.copyProducts(List.of(input), UUID.fromString(inv.getId()))).thenReturn(true);
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.addProducts))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "products", List.of(Map.of(
        "refId", input.getRefId(),
        "stock", input.getStock()
      )),
      "idTo", inv.getId()
    );

    String query = """
      mutation($products: [ProductToCopyInput]!, $idTo: ID!) {
        copyProducts(products: $products, idTo: $idTo)
      }
    """;

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).variables(variables).execute().path("copyProducts").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).copyProducts(List.of(input), UUID.fromString(inv.getId()));
  }

  @Test
  void copyProducts_deniedIfHasWrongPerm() {
    ProductToCopyDTO input = ProductToCopyDTO.builder()
      .refId(UUID.randomUUID().toString())
      .stock(4)
    .build();
    
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.editInventory))
      .build()
    ));

    Map<String, Object> variables = Map.of(
      "products", List.of(Map.of(
        "refId", input.getRefId(),
        "stock", input.getStock()
      )),
      "idTo", inv.getId()
    );

    String query = """
      mutation($products: [ProductToCopyInput]!, $idTo: ID!) {
        copyProducts(products: $products, idTo: $idTo)
      }
    """;

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).variables(variables).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).copyProducts(anyList(), any());
  }

  // TODO: test for every method, should deny if no user account is logged

  @Test
  void editStockOfProduct_allowIfHasRightPerm() {
    UUID refId = UUID.randomUUID();
    when(inventoryService.editStockOfProduct(2, refId, UUID.fromString(inv.getId()))).thenReturn(true);
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.editInventory))
      .build()
    ));

    String query = """
      mutation {
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s")
      }
    """.formatted(refId.toString(), inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().path("editStockOfProduct").entity(Boolean.class).isEqualTo(true);

    verify(inventoryService).editStockOfProduct(2, refId, UUID.fromString(inv.getId()));
  }

  @Test
  void editStockOfProduct_deniedIfHasWrongPerm() {
    UUID refId = UUID.randomUUID();
    configJwtMock("user", "role", false, List.of(
      InventoryPermsDTO.builder()
        .idOfInventoryReferenced(inv.getId())
        .permissions(List.of(Permissions.addProducts))
      .build()
    ));

    String query = """
      mutation {
        editStockOfProduct(relativeNewStock: 2, productRefId: "%s", invId: "%s")
      }
    """.formatted(refId.toString(), inv.getId());

    graphQlTester.mutate().url("http://localhost:" + port + "/graphql").headers(generateHeaderWithToken()).build()
      .document(query).execute().errors()
    .satisfy(errors -> {
      assertFalse(errors.isEmpty());
      assertTrue(errors.get(0).getMessage().contains("Forbidden"));
    });

    verify(inventoryService, never()).editStockOfProduct(anyInt(), any(), any());
  }
}
