package com.nahuelgg.inventory_app.inventories.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.AccountFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.services.implementations.InventoryService_Impl;
import com.nahuelgg.inventory_app.inventories.utilities.Mappers;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
  @Mock InventoryRepository inventoryRepository;
  @Mock ProductInInvRepository productInInvRepository;
  @Mock RestTemplate restTemplate;

  @InjectMocks InventoryService_Impl inventoryService;

  UUID accId = UUID.randomUUID();

  InventoryDTO invDTO1;
  InventoryEntity invEntity1;

  ProductFromProductsMSDTO pFromProductsMSDTO1, pFromProductsMSDTO2;
  ProductInInvEntity pInInvEntity1, pInInvEntity2;
  ProductInInvDTO pInInvDTO1, pInInvDTO2;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(inventoryService, "mappers", new Mappers());

    pFromProductsMSDTO1 = ProductFromProductsMSDTO.builder()
      .id(UUID.randomUUID().toString())
      .name("product1")
      .brand("brand1")
      .accountId(accId.toString())
    .build();
    pFromProductsMSDTO2 = ProductFromProductsMSDTO.builder()
      .id(UUID.randomUUID().toString())
      .name("product2")
      .brand("brand2")
      .accountId(accId.toString())
    .build();

    pInInvEntity1 = ProductInInvEntity.builder()
      .id(UUID.randomUUID())
      .referenceId(UUID.fromString(pFromProductsMSDTO1.getId()))
      .stock(4)
      .isAvailable(true)
    .build();
    pInInvEntity2 = ProductInInvEntity.builder()
      .id(UUID.randomUUID())
      .referenceId(UUID.fromString(pFromProductsMSDTO2.getId()))
      .stock(0)
      .isAvailable(false)
    .build();

    pInInvDTO1 = ProductInInvDTO.builder()
      .refId(pFromProductsMSDTO1.getId())
      .name(pFromProductsMSDTO1.getName())
      .brand(pFromProductsMSDTO1.getBrand())
      .stock(pInInvEntity1.getStock())
      .isAvailable(pInInvEntity1.getIsAvailable())
    .build();
    pInInvDTO2 = ProductInInvDTO.builder()
      .refId(pFromProductsMSDTO2.getId())
      .name(pFromProductsMSDTO2.getName())
      .brand(pFromProductsMSDTO2.getBrand())
      .stock(pInInvEntity2.getStock())
      .isAvailable(pInInvEntity2.getIsAvailable())
    .build();

    invEntity1 = InventoryEntity.builder()
      .id(UUID.randomUUID())
      .name("inventory1")
      .accountId(accId)
      .products(new ArrayList<>(List.of(pInInvEntity1, pInInvEntity2)))
      // .userReferences(new ArrayList<>())
    .build();
    
    invDTO1 = InventoryDTO.builder()
      .id(invEntity1.getId().toString())
      .name("inventory1")
      .accountId(accId.toString())
      .products(new ArrayList<>(List.of(pInInvDTO1, pInInvDTO2)))
      // .usersIds(new ArrayList<>())
    .build();

    pInInvEntity1.setInventory(invEntity1);
    pInInvEntity2.setInventory(invEntity1);
  }

  void configRestTemplateToGetProductsFromIds() {
    setContextAuth();
    when(restTemplate.exchange(
      anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())
    ).thenReturn(new ResponseEntity<>(ResponseDTO.builder().data(List.of(pFromProductsMSDTO1, pFromProductsMSDTO2)).build(), HttpStatus.OK));
  }

  private void setContextAuth() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("userInfo", "token"));
  }

  void testRestRequest(String urlToCheck) {
    ArgumentCaptor<String> urlToProductMicroservice = ArgumentCaptor.forClass(String.class);
    verify(restTemplate).exchange(urlToProductMicroservice.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());

    assertTrue(urlToProductMicroservice.getValue().contains(urlToCheck));
  }

  @Test 
  void getById_returnsExpectedDTO() {
    when(inventoryRepository.findById(invEntity1.getId())).thenReturn(Optional.of(invEntity1));
    configRestTemplateToGetProductsFromIds();
    
    assertEquals(invDTO1, inventoryService.getById(invEntity1.getId()));
    verify(inventoryRepository).findById(invEntity1.getId());
    
    testRestRequest("http://api-products:8081/product/ids?list=");
  }

  @Test 
  void getByAccount_returnsExpectedList() {
    when(inventoryRepository.findByAccountId(accId)).thenReturn(List.of(invEntity1));
    configRestTemplateToGetProductsFromIds();

    assertIterableEquals(List.of(invDTO1), inventoryService.getByAccount(accId));
    verify(inventoryRepository).findByAccountId(accId);

    testRestRequest("http://api-products:8081/product/ids?list=");
  }

  @Test
  void searchProductsInInventories_callsProductsMicroserviceAndReturnsExpected() {
    List<UUID> refIdList = List.of(UUID.fromString(pFromProductsMSDTO2.getId()));

    setContextAuth();
    when(restTemplate.exchange(
      anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())
    ).thenReturn(
      new ResponseEntity<>(ResponseDTO.builder().data(List.of(pFromProductsMSDTO2)).build(), HttpStatus.OK)
    );
    when(inventoryRepository.searchByProductRefId(refIdList)).thenReturn(List.of(invEntity1));

    InventoryDTO expected = invDTO1.toBuilder()
      .products(List.of(pInInvDTO2))
    .build();

    assertIterableEquals(List.of(expected), inventoryService.searchProductsInInventories("product2", null, null, null, accId));
    verify(inventoryRepository).searchByProductRefId(refIdList);

    testRestRequest("http://api-products:8081/product/search");
  }

  @Test
  void create_returnsExpectedAndMakeRightCalls() {
    String name = "inventory2";

    InventoryEntity invSaved = InventoryEntity.builder()
      .id(UUID.randomUUID())
      .name(name)
      .accountId(accId)
    .build();
    InventoryDTO expected = InventoryDTO.builder()
      .id(invSaved.getId().toString())
      .name(name)
      .accountId(accId.toString())
      .products(List.of())
    .build();

    setContextAuth();
    when(inventoryRepository.existsByNameAndAccountId(name, accId)).thenReturn(false);
    when(restTemplate.exchange(
      anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())
    ).thenReturn(
      new ResponseEntity<>(
        ResponseDTO.builder().data(
          AccountFromUsersMSDTO.builder().id(accId.toString()).build()
        ).build(), 
        HttpStatus.OK
      )
    );
    when(inventoryRepository.save(any(InventoryEntity.class))).thenReturn(invSaved);

    InventoryDTO actual = inventoryService.create(name, accId);

    assertEquals(expected, actual);
    verify(inventoryRepository).save(any(InventoryEntity.class));

    testRestRequest("http://api-users:8082/account/add-inventory");
  }

  @Test
  void edit_returnsSavedDtoWithChanges() {
    InventoryEntity invWithChanges = invEntity1.toBuilder()
      .name("newName")
    .build();

    when(inventoryRepository.findById(invEntity1.getId())).thenReturn(Optional.of(invEntity1));
    ArgumentCaptor<InventoryEntity> invSaved = ArgumentCaptor.forClass(InventoryEntity.class);
    when(inventoryRepository.save(invSaved.capture())).thenReturn(invWithChanges);

    boolean result = inventoryService.edit(invEntity1.getId(), "newName");

    assertTrue(result);
    assertEquals(invWithChanges, invSaved.getValue());
    verify(inventoryRepository).findById(invEntity1.getId());
    verify(inventoryRepository).save(any(InventoryEntity.class));
  }

  @Test
  void addProduct_saveRightEntitiesAndMakeRightCall() {
    String productName = "productCreated";
    ProductInputDTO productToCreate = ProductInputDTO.builder()
      .name(productName)
      .stock(4)
    .build();
    ProductFromProductsMSDTO productCreatedInMicroservice = ProductFromProductsMSDTO.builder()
      .id(UUID.randomUUID().toString())
      .name(productName)
    .build();

    ProductInInvEntity expectedSaved = ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productCreatedInMicroservice.getId()))
      .stock(4)
      .isAvailable(true)
      .inventory(invEntity1)
    .build();

    when(inventoryRepository.findById(invEntity1.getId())).thenReturn(Optional.of(invEntity1));
    ArgumentCaptor<ProductInInvEntity> pInInvSaved = ArgumentCaptor.forClass(ProductInInvEntity.class);
    when(productInInvRepository.save(pInInvSaved.capture())).thenReturn(expectedSaved);

    setContextAuth();
    when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(
      new ResponseEntity<ResponseDTO>(
        ResponseDTO.builder().data(productCreatedInMicroservice).build(), 
        HttpStatus.CREATED
      )
    );

    inventoryService.addProduct(productToCreate, invEntity1.getId(), accId);

    assertEquals(expectedSaved, pInInvSaved.getValue());
    testRestRequest("http://api-products:8081/product?invId=" + invEntity1.getId().toString());
  }

  @Test
  void copyProducts_addsProductsToDestinyInv() {
    List<ProductToCopyDTO> inputList = invEntity1.getProducts().stream().map(
      pInInvE -> new ProductToCopyDTO(pInInvE.getReferenceId().toString(), 1)
    ).toList();

    UUID destinyInvId = UUID.randomUUID();
    InventoryEntity destinyInv = InventoryEntity.builder()
      .id(destinyInvId)
      .name("inventory2")
      .accountId(accId)
      .products(new ArrayList<>())
    .build();

    List<ProductInInvEntity> expectedSavedList = List.of(
      pInInvEntity1.toBuilder().id(null).inventory(destinyInv).stock(1).build(),
      pInInvEntity2.toBuilder().id(null).inventory(destinyInv).stock(1).isAvailable(true).build()
    );

    when(inventoryRepository.findById(destinyInvId)).thenReturn(Optional.of(destinyInv));
    ArgumentCaptor<List<ProductInInvEntity>> savedList = ArgumentCaptor.forClass(List.class);
    when(productInInvRepository.saveAll(savedList.capture())).thenReturn(expectedSavedList);

    inventoryService.copyProducts(inputList, destinyInvId);

    assertIterableEquals(expectedSavedList, savedList.getValue());
  }

  @Test
  void editStockOfProduct_saveNewEntityWithRightChange() {
    when(productInInvRepository.findByReferenceIdAndInventoryId(pInInvEntity1.getReferenceId(), invEntity1.getId())).thenReturn(Optional.of(pInInvEntity1));

    inventoryService.editStockOfProduct(2, pInInvEntity1.getReferenceId(), invEntity1.getId());

    ArgumentCaptor<ProductInInvEntity> savedProductInInv = ArgumentCaptor.forClass(ProductInInvEntity.class);
    verify(productInInvRepository, times(1)).save(savedProductInInv.capture());
    assertEquals(6, savedProductInInv.getValue().getStock());
  }

  @Test
  void delete_makeRightCalls() {
    when(inventoryRepository.findById(invEntity1.getId())).thenReturn(Optional.of(invEntity1));
    when(productInInvRepository.findByInventory(invEntity1)).thenReturn(List.of(pInInvEntity1));
    when(productInInvRepository.findReferenceIdsExclusiveToInventory(invEntity1.getId(), accId)).thenReturn(List.of(pInInvEntity1.getReferenceId()));

    setContextAuth();
    when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(
      new ResponseEntity<>(
        ResponseDTO.builder().build(),
        HttpStatus.OK
      )
    );
    
    inventoryService.delete(invEntity1.getId(), accId);

    ArgumentCaptor<String> usedUrls = ArgumentCaptor.forClass(String.class);
    verify(restTemplate, times(2)).exchange(usedUrls.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());
    List<String> listOfUrls = usedUrls.getAllValues();
    assertTrue(listOfUrls.get(0).contains("http://api-users:8082/account/remove-inventory"));
    assertTrue(listOfUrls.get(1).contains("http://api-products:8081/product/delete-by-ids"));
    assertTrue(listOfUrls.get(1).contains(pInInvEntity1.getReferenceId().toString()));

    verify(inventoryRepository).deleteById(invEntity1.getId());
    verify(productInInvRepository).deleteAll(List.of(pInInvEntity1));
  }

  @Test
  void deleteByAccountId_callsRepository() {
    when(inventoryRepository.findByAccountId(accId)).thenReturn(List.of(invEntity1));
    inventoryService.deleteByAccountId(accId);
    verify(inventoryRepository).deleteAll(List.of(invEntity1));
  }
}
