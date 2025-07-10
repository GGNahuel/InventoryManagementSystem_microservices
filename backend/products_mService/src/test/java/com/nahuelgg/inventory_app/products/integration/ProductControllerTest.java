package com.nahuelgg.inventory_app.products.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.products.dtos.ProductDTO;
import com.nahuelgg.inventory_app.products.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.enums.Permissions;
import com.nahuelgg.inventory_app.products.services.JwtService;
import com.nahuelgg.inventory_app.products.services.ProductService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProductControllerTest {
  @Autowired TestRestTemplate restTemplate;
  
  @MockitoBean ProductService service;
  @MockitoBean JwtService jwtService;

  UUID acc1ID = UUID.randomUUID();
  ProductEntity pr1;
  ProductDTO prDTO1;

  String accUsername = "accUsername";
  String token = "testToken";
  String invId = "abcde";

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

    prDTO1 = ProductDTO.builder()
      .id(pr1.getId().toString())
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories(List.of("cat1"))
      .accountId(acc1ID.toString())
    .build();
  }

  private void configJwtMock(String userName, String userRole, boolean isAdmin, List<PermissionsForInventoryDTO> userPerms) {
    when(jwtService.getClaim(eq(token), any())).thenAnswer(inv -> {
      Function<Claims, ?> claimGetter = inv.getArgument(1);
      Claims claims = Jwts.claims();
      claims.setSubject(accUsername);
      claims.put("accountId", acc1ID.toString());
      claims.put("userName", userName);
      claims.put("userRole", userRole);
      claims.put("isAdmin", isAdmin);
      claims.put("userPerms", userPerms);
      return claimGetter.apply(claims);
    });
    when(jwtService.isTokenExpired(token)).thenReturn(false);
  }

  private HttpHeaders generateHeaderWithToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    return headers;
  }

  @Test
  void getByIds() {
    when(service.getByIds(anyList())).thenReturn(List.of(prDTO1));
    configJwtMock(null, null, false, null);

    String uri = UriComponentsBuilder.fromUriString("/product/ids")
      .queryParam("list", List.of(prDTO1.getId()).toArray())
    .toUriString();
    ResponseEntity<ResponseDTO<List<ProductDTO>>> response = restTemplate.exchange(
      uri, HttpMethod.GET, 
      new HttpEntity<>(generateHeaderWithToken()), 
      new ParameterizedTypeReference<ResponseDTO<List<ProductDTO>>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ResponseDTO<List<ProductDTO>> responseDTO = response.getBody();
    List<ProductDTO> actualData = responseDTO.getData();

    assertNull(responseDTO.getError());
    assertNotNull(responseDTO.getData());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void search() {
    when(service.search(any(), any(), any(), any(), any())).thenReturn(List.of(prDTO1));
    configJwtMock(null, null, false, null);

    String uri = UriComponentsBuilder.fromUriString("/product/search")
      .queryParam("brand", "Marca 1")
      .queryParam("name", "Ventilador")
      .queryParam("categoryNames", "cat1")
      .queryParam("accountId", acc1ID.toString())
      .toUriString();
    ResponseEntity<ResponseDTO<List<ProductDTO>>> response = restTemplate.exchange(
      uri, HttpMethod.GET, 
      new HttpEntity<>(generateHeaderWithToken()), 
      new ParameterizedTypeReference<ResponseDTO<List<ProductDTO>>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ResponseDTO<List<ProductDTO>> responseDTO = response.getBody();
    List<ProductDTO> actualData = responseDTO.getData();

    assertNull(responseDTO.getError());
    assertIterableEquals(List.of(prDTO1), actualData);
  }

  @Test
  void create_successWithRightPerm() {
    when(service.create(any())).thenReturn(prDTO1);

    configJwtMock(
      "user", "role", false, 
      List.of(PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.addProducts))
      .build())
    );

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product?invId=" + invId, HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode(), "El check de user con el perm ha fallado");

    ResponseDTO<ProductDTO> responseDTO = response.getBody();
    ProductDTO actual = responseDTO.getData();

    assertNull(responseDTO.getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void create_successIfAdmin() {
    when(service.create(any())).thenReturn(prDTO1);
    
    configJwtMock("admin", "admin", true, null);

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product?invId=" + invId, HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(201), response.getStatusCode(), "El check de admin ha fallado");

    ResponseDTO<ProductDTO> responseDTO = response.getBody();
    ProductDTO actual = responseDTO.getData();

    assertNull(responseDTO.getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void create_denied() {
    configJwtMock(
      "user", "role", false, 
      List.of(PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.editInventory))
      .build())
    );

    HttpEntity<ProductDTO> request = new HttpEntity<ProductDTO>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      "/product?invId=" + invId, HttpMethod.POST, request, 
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(service, never()).create(any());
  }

  @Test
  void update_successWithRightPerm() {
    when(service.update(any())).thenReturn(prDTO1);
    configJwtMock(
      "user", "role", false,
      List.of(PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.editProducts))
      .build())
    );

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit?invId=" + invId, HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void update_successIfAdmin() {
    when(service.update(any())).thenReturn(prDTO1);
    configJwtMock("user", "role", true, List.of());

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<ProductDTO>> response = restTemplate.exchange(
      "/product/edit?invId=" + invId, HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<ProductDTO>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    ProductDTO actual = response.getBody().getData();

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getError());
    assertEquals(prDTO1, actual);
  }

  @Test
  void update_denied() {
    when(service.update(any())).thenReturn(prDTO1);
    configJwtMock("user", "role", false, List.of());

    HttpEntity<ProductDTO> request = new HttpEntity<>(prDTO1, generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      "/product/edit?invId=" + invId, HttpMethod.PUT, request, 
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(service, never()).update(any());
  }

  @Test
  void delete_successWithRightPerm() {
    configJwtMock(
      "user", "role", false, 
      List.of(PermissionsForInventoryDTO.builder()
        .idOfInventoryReferenced(invId)
        .permissions(List.of(Permissions.deleteProducts))
      .build())
    );

    String uri = "/product?id=" + prDTO1.getId() + "&invId=" + invId;
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request, 
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    assertEquals("Producto eliminado con éxito", response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void delete_successIfAdmin() {
    configJwtMock("user", "role", true, null);

    String uri = "/product?id=" + prDTO1.getId() + "&invId=" + invId;
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());

    assertEquals("Producto eliminado con éxito", response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void delete_denied() {
    configJwtMock("user", "role", false, null);

    String uri = "/product?id=" + prDTO1.getId() + "&invId=" + invId;
    HttpEntity<String> request = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<String>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, request,
      new ParameterizedTypeReference<ResponseDTO<String>>() {}
    );

    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
    verify(service, never()).delete(any());
  }

  @Test
  void deleteByAccountId_success() {
    configJwtMock("user", "role", true, null);
    String uri = "/product/delete-by-account?id=" + acc1ID;

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByAccountId_denied() {
    configJwtMock("user", "role", false, null);
    String uri = "/product/delete-by-account?id=" + acc1ID;

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(403, response.getBody().getStatus());
    verify(service, never()).deleteByAccountId(any());
  }

  @Test
  void deleteByIds_success() {
    configJwtMock("user", "role", true, null);
    String uri = UriComponentsBuilder.fromUriString("/product/delete-by-ids")
      .queryParam("ids", List.of(prDTO1.getId()))
    .toUriString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(200, response.getBody().getStatus());
    assertNull(response.getBody().getData());
    assertNull(response.getBody().getError());
  }

  @Test
  void deleteByIds_denied() {
    configJwtMock("user", "role", false, null);

    String uri = UriComponentsBuilder.fromUriString("/product/delete-by-ids")
      .queryParam("ids", List.of(prDTO1.getId()))
    .toUriString();

    HttpEntity<String> entity = new HttpEntity<>(generateHeaderWithToken());
    ResponseEntity<ResponseDTO<Object>> response = restTemplate.exchange(
      uri, HttpMethod.DELETE, entity,
      new ParameterizedTypeReference<ResponseDTO<Object>>() {}
    );

    assertEquals(403, response.getBody().getStatus());
    verify(service, never()).deleteByIds(anyList());
  }
}
