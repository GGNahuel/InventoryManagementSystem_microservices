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

import com.nahuelgg.inventory_app.inventories.dtos.AccountFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.repositories.UserReferenceRepository;
import com.nahuelgg.inventory_app.inventories.services.implementations.InventoryService_Impl;
import com.nahuelgg.inventory_app.inventories.utilities.Mappers;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
  @Mock InventoryRepository inventoryRepository;
  @Mock ProductInInvRepository productInInvRepository;
  @Mock UserReferenceRepository userReferenceRepository;
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
      .users(new ArrayList<>())
    .build();
    
    invDTO1 = InventoryDTO.builder()
      .id(invEntity1.getId().toString())
      .name("inventory1")
      .accountId(accId.toString())
      .products(new ArrayList<>(List.of(pInInvDTO1, pInInvDTO2)))
      .usersIds(new ArrayList<>())
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

    InventoryEntity firstSave = InventoryEntity.builder().id(UUID.randomUUID()).name(name).build();
    InventoryEntity lastSave = InventoryEntity.builder()
      .id(firstSave.getId())
      .name(name)
      .accountId(accId)
      .users(List.of())
    .build();
    InventoryDTO expected = InventoryDTO.builder()
      .id(lastSave.getId().toString())
      .name(name)
      .accountId(accId.toString())
      .usersIds(List.of())
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
    when(inventoryRepository.save(InventoryEntity.builder().name(name).build())).thenReturn(firstSave);
    when(inventoryRepository.save(lastSave)).thenReturn(lastSave);

    InventoryDTO actual = inventoryService.create(name, accId);

    assertEquals(expected, actual);
    verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));

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
  void addUser_saveUserRefEntityAndChangesInInvEntity() {
    UUID userRefId = UUID.randomUUID();
    UserReferenceEntity newUser = UserReferenceEntity.builder().id(UUID.randomUUID()).referenceId(userRefId).build();

    when(inventoryRepository.findById(invEntity1.getId())).thenReturn(Optional.of(invEntity1));
    ArgumentCaptor<UserReferenceEntity> userRefSaved = ArgumentCaptor.forClass(UserReferenceEntity.class);
    when(userReferenceRepository.save(userRefSaved.capture())).thenReturn(newUser);

    inventoryService.addUser(userRefId, invEntity1.getId());

    assertEquals(userRefId, userRefSaved.getValue().getReferenceId());

    ArgumentCaptor<InventoryEntity> invSaved = ArgumentCaptor.forClass(InventoryEntity.class);
    verify(inventoryRepository).save(invSaved.capture());
    assertTrue(invSaved.getValue().getUsers().contains(newUser));
  }

  @Test
  void removeUser_makeRightChanges() {
    UserReferenceEntity userToRemove = UserReferenceEntity.builder()
      .id(UUID.randomUUID())
      .referenceId(UUID.randomUUID())
    .build();
    UserReferenceEntity user2 = UserReferenceEntity.builder()
      .id(UUID.randomUUID())
      .referenceId(UUID.randomUUID())
    .build();

    InventoryEntity anotherInv = InventoryEntity.builder()
      .id(UUID.randomUUID())
      .name("inventory2")
      .accountId(accId)
      .users(new ArrayList<>(List.of(userToRemove, user2)))
      .products(List.of())
    .build();
    invEntity1.getUsers().add(user2);

    List<InventoryEntity> expectedChanges = List.of(invEntity1, anotherInv).stream().map(
      invEnt -> {
        invEnt.setUsers(invEnt.getUsers().stream().filter(
          user -> user.getReferenceId() != userToRemove.getReferenceId()
        ).toList());

        return invEnt;
      }
    ).toList();

    when(inventoryRepository.findByAccountId(accId)).thenReturn(List.of(invEntity1, anotherInv));

    inventoryService.removeUser(userToRemove.getId(), accId);

    ArgumentCaptor<List<InventoryEntity>> savedList = ArgumentCaptor.forClass(List.class);
    verify(inventoryRepository).saveAll(savedList.capture());
    assertIterableEquals(expectedChanges, savedList.getValue());
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

    inventoryService.addProduct(productToCreate, invEntity1.getId());

    assertEquals(expectedSaved, pInInvSaved.getValue());

    ArgumentCaptor<InventoryEntity> invSaved = ArgumentCaptor.forClass(InventoryEntity.class);
    verify(inventoryRepository).save(invSaved.capture());
    assertTrue(invSaved.getValue().getProducts().contains(expectedSaved));

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
      .users(invEntity1.getUsers())
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
    ArgumentCaptor<InventoryEntity> savedInv = ArgumentCaptor.forClass(InventoryEntity.class);
    verify(inventoryRepository).save(savedInv.capture());
    assertIterableEquals(expectedSavedList, savedInv.getValue().getProducts());
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
    when(productInInvRepository.findReferenceIdsExclusiveToInventory(invEntity1.getId())).thenReturn(List.of(pInInvEntity1.getReferenceId()));

    setContextAuth();
    when(restTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any())).thenReturn(
      new ResponseEntity<>(
        ResponseDTO.builder().build(),
        HttpStatus.OK
      )
    );
    
    inventoryService.delete(invEntity1.getId());

    ArgumentCaptor<String> usedUrls = ArgumentCaptor.forClass(String.class);
    verify(restTemplate, times(2)).exchange(usedUrls.capture(), any(), any(), ArgumentMatchers.<Class<ResponseDTO>>any());
    List<String> listOfUrls = usedUrls.getAllValues();
    assertTrue(listOfUrls.get(0).contains("http://api-users:8082/account/remove-inventory"));
    assertTrue(listOfUrls.get(1).contains("http://api-products:8081/delete-by-ids"));
    assertTrue(listOfUrls.get(1).contains(pInInvEntity1.getReferenceId().toString()));

    verify(inventoryRepository).deleteById(invEntity1.getId());
  }

  @Test
  void deleteByAccountId_callsRepository() {
    when(inventoryRepository.findByAccountId(accId)).thenReturn(List.of(invEntity1));
    inventoryService.deleteByAccountId(accId);
    verify(inventoryRepository).deleteAll(List.of(invEntity1));
  }
}
