package com.nahuelgg.inventory_app.inventories.services.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.AccountFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.EditProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.repositories.UserReferenceRepository;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;
import com.nahuelgg.inventory_app.inventories.utilities.Mappers;

@Service
public class InventoryService_Impl implements InventoryService {
  private final InventoryRepository repository;
  private final ProductInInvRepository productInvRepository;
  private final UserReferenceRepository userRefRepository;
  private final RestTemplate restTemplate;
  private final Mappers mappers = new Mappers();

  public InventoryService_Impl(
    InventoryRepository repository, ProductInInvRepository productInInvRepository, UserReferenceRepository userRefRepository,
    RestTemplate restTemplate
  ) {
    this.repository = repository;
    this.productInvRepository = productInInvRepository;
    this.userRefRepository = userRefRepository;
    this.restTemplate = restTemplate;
  }

  private HttpHeaders setTokenToOtherServicesRequests() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null)
      throw new RuntimeException("No se encontró autenticación para realizar la operación");
    
    HttpHeaders header = new HttpHeaders();
    String tokenFromAuth = (String) auth.getCredentials();
    header.setBearerAuth(tokenFromAuth);

    return header;
  }

  private ResponseDTO makeRestRequest(String url, HttpMethod method, Object optionalBody) {
    ResponseEntity<ResponseDTO> response;
    try {
      HttpEntity<Object> entity = optionalBody != null ? new HttpEntity<Object>(optionalBody, setTokenToOtherServicesRequests()) : new HttpEntity<>(setTokenToOtherServicesRequests());

      response = restTemplate.exchange(url, method, entity, ResponseDTO.class);
      if (response.getStatusCode() != HttpStatusCode.valueOf(200) && response.getStatusCode() != HttpStatusCode.valueOf(201))
        throw new RuntimeException(response.getBody().getError().toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return response.getBody();
  }

  private List<ProductFromProductsMSDTO> getProductsFromMS(InventoryEntity inv) {
    List<String> productsId = inv.getProducts().stream().map(
      pInInvEntity -> pInInvEntity.getReferenceId().toString()
    ).toList();

    String baseUrl = "http://api-products:8081/product/ids";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("list", productsId.toArray())
    .toUriString();

    ResponseDTO responseDto = makeRestRequest(completeUrl, HttpMethod.GET, null);

    return (List<ProductFromProductsMSDTO>) responseDto.getData();
  }

  @Override @Transactional(readOnly = true)
  public InventoryDTO getById(UUID id) {
    InventoryEntity inv = repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    );
    List<ProductFromProductsMSDTO> productsFromMS = getProductsFromMS(inv);

    return mappers.mapInvEntity(inv, productsFromMS);
  }

  @Override @Transactional(readOnly = true)
  public List<InventoryDTO> getByAccount(UUID accountId) {
    return repository.findByAccountId(accountId).stream().map(
      inv -> mappers.mapInvEntity(inv, getProductsFromMS(inv))
    ).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<InventoryDTO> searchProductsInInventories(
    String name, String brand, String model, List<String> categories, UUID accountId
  ) {
    String baseUrl = "http://api-products:8081/product/search";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("brand", brand)
      .queryParam("name", name)
      .queryParam("model", model)
      .queryParam("categoryNames", categories != null ? categories.toArray() : null)
      .queryParam("accountId", accountId.toString())
    .toUriString(); 
    List<ProductFromProductsMSDTO> resultsOfProducts = (List<ProductFromProductsMSDTO>) makeRestRequest(completeUrl, HttpMethod.GET, null).getData();

    List<InventoryDTO> inventoriesWithThoseProducts = repository.searchByProductRefId(
      resultsOfProducts.stream().map(
        p -> UUID.fromString(p.getId())
      ).toList()
    ).stream().map(
      i -> mappers.mapInvEntity(i, resultsOfProducts)
    ).toList();

    inventoriesWithThoseProducts = inventoriesWithThoseProducts.stream().map(
      invDto -> invDto.toBuilder()
        .products(invDto.getProducts().stream().filter(
          productInDto -> resultsOfProducts.stream().anyMatch(
            originalProduct -> originalProduct.getId() == productInDto.getRefId()
          )
        ).toList())
      .build()
    ).toList();

    return inventoriesWithThoseProducts;
  }

  // mutations
  @Override @Transactional
  public InventoryDTO create(String name, UUID accountId) {
    //TODO: extraer accId del contexto de seguridad
    if (repository.existsByNameAndAccountId(name, accountId))
      throw new RuntimeException("Ya existe un inventario con ese nombre en la cuenta");

    InventoryEntity inv = repository.save(InventoryEntity.builder().name(name).accountId(accountId).build());

    String baseUrl = "http://api-users:8082/account/add-inventory";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("accountId", accountId.toString())
      .queryParam("invId", inv.getId().toString())
    .toUriString();
    AccountFromUsersMSDTO account = (AccountFromUsersMSDTO) makeRestRequest(completeUrl, HttpMethod.PATCH, null).getData();

    inv.setAccountId(UUID.fromString(account.getId()));
    inv.setUsers(account.getUsers() != null ? account.getUsers().stream().map(
      u -> userRefRepository.findByReferenceId(UUID.fromString(u.getId())).orElse(
        userRefRepository.save(
          UserReferenceEntity.builder().referenceId(UUID.fromString(u.getId())).build()
        )
      )
    ).toList() : new ArrayList<>());

    return mappers.mapInvEntity(repository.save(inv), List.of());
  }

  @Override @Transactional
  public boolean edit(UUID id, String name) {
    InventoryEntity inv = repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    );
    inv.setName(name);
    repository.save(inv);
    return true;
  }

  @Override @Transactional
  public boolean addUser(UUID userId, UUID invId) {
    InventoryEntity inv = repository.findById(invId).orElseThrow(
      () -> new RuntimeException("")
    );
    List<UserReferenceEntity> userRefs = inv.getUsers();

    if (userRefs.stream().anyMatch(uRefEntity -> uRefEntity.getReferenceId() == userId)) 
      return false;

    userRefs.add(userRefRepository.save(
      UserReferenceEntity.builder().referenceId(userId).build()
    ));
    inv.setUsers(userRefs);

    repository.save(inv);
    return true;
  }

  @Override @Transactional
  public boolean removeUser(UUID userId, UUID accountId) {
    List<InventoryEntity> invs = repository.findByAccountId(accountId);
    for (InventoryEntity inv : invs) {
      System.out.println(inv.toString());
      System.out.println(inv.getUsers().stream().filter(
        userRefEntity -> userRefEntity.getReferenceId() != userId 
      ).collect(Collectors.toList()).toString());

      inv.setUsers(inv.getUsers().stream().filter(
        userRefEntity -> !(userRefEntity.getReferenceId().equals(userId))
      ).collect(Collectors.toList()));
    }
    repository.saveAll(invs);

    return true;
  }

  @Override @Transactional
  public ProductInInvDTO addProduct(ProductInputDTO productInput, UUID invId, UUID accountId) {
    // TODO: (optional) agregar validación de si ese producto ya está creado y agregado al inventario, necesitaría un cambio en la DB de products?
    InventoryEntity inv = repository.findById(invId).orElseThrow(
      () -> new RuntimeException("Inv not found")
    );
      
    String baseUrl = "http://api-products:8081/product?invId=%s&accountId=%s".formatted(invId.toString(), accountId.toString());
    ProductFromProductsMSDTO productCreated = (ProductFromProductsMSDTO) makeRestRequest(
      baseUrl, HttpMethod.POST, mappers.mapProductInput(productInput))
    .getData();

    int checkedStock = productInput.getStock() != null ? productInput.getStock() : 0;
    ProductInInvEntity newProductInv = productInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productCreated.getId()))
      .stock(checkedStock)
      .isAvailable(productInput.getStock() > 0)
      .inventory(inv)
    .build());

    return mappers.mapProductsFromMSToDTO(productCreated, newProductInv);
  }

  @Override
  public ProductInInvDTO editProductInInventory(EditProductInputDTO product, UUID invId, UUID accountId) {
    repository.findById(invId).orElseThrow(
      () -> new RuntimeException("Inv not found")
    );
    ProductInInvEntity productToEdit = productInvRepository.findByReferenceIdAndInventoryId(UUID.fromString(product.getRefId()), invId).orElseThrow(
      () -> new RuntimeException("Producto a editar no encontrado")
    );

    ProductFromProductsMSDTO editedProduct;

    if (productInvRepository.findReferenceIdsExclusiveToInventory(invId, accountId).contains(productToEdit.getReferenceId())) {
      String baseUrl = "http://api-products:8081/product/edit/common-perm?invId=%s&accountId=%s".formatted(invId.toString(), accountId.toString());

      editedProduct = (ProductFromProductsMSDTO) makeRestRequest(
        baseUrl, HttpMethod.POST, product.mapToProductFromProductService(accountId.toString())
      ).getData();
    } else {
      String baseUrl = "http://api-products:8081/product?invId=%s&accountId=%s".formatted(invId.toString(), accountId.toString());

      editedProduct = (ProductFromProductsMSDTO) makeRestRequest(
        baseUrl, HttpMethod.POST, product.mapToProductFromProductService(accountId.toString())
      ).getData();
        
      productToEdit.setReferenceId(UUID.fromString(editedProduct.getId()));
      productInvRepository.save(productToEdit);
    }
    
    return mappers.mapProductsFromMSToDTO(editedProduct, productToEdit);
  }

  //TODO: chequear si en los llamados internos se pasan todos los parametros, incluidos el accId

  @Override @Transactional
  public boolean copyProducts(List<ProductToCopyDTO> products, UUID idTo) {
    InventoryEntity invTo = repository.findById(idTo).orElseThrow(
      () -> new RuntimeException("")
    );

    List<ProductInInvEntity> newList = invTo.getProducts();
    for (ProductToCopyDTO p : products) {
      if (!newList.stream().filter(pInv -> p.getRefId().equals(pInv.getReferenceId().toString())).findFirst().isPresent()) {
        newList.add(ProductInInvEntity.builder()
          .referenceId(UUID.fromString(p.getRefId()))
          .stock(p.getStock())
          .isAvailable(p.getStock() > 0)
          .inventory(invTo)
        .build());
      }
    }
    productInvRepository.saveAll(newList);

    return true;
  }

  @Override @Transactional
  public boolean editStockOfProduct(int relativeNewStock, UUID productRefId, UUID invId) {
    ProductInInvEntity p = productInvRepository.findByReferenceIdAndInventoryId(productRefId, invId).orElseThrow(
      () -> new RuntimeException("")
    );
    int newStock = p.getStock() + relativeNewStock;
    p.setStock(newStock < 0 ? 0 : newStock);
    p.setIsAvailable(newStock > 0);

    productInvRepository.save(p);
    return true;
  }

  @Override
  public boolean deleteProductInInventory(List<UUID> productRefIds, UUID invId, UUID accountId) {
    InventoryEntity inv = repository.findById(invId).orElseThrow(
      () -> new RuntimeException("inv not found")
    );
    
    List<ProductInInvEntity> productsInInv = inv.getProducts();
    List<UUID> pRefIdsInInventory = productsInInv.stream().map(pInInv -> pInInv.getReferenceId()).toList();

    if (!productRefIds.stream().allMatch(
      pRefId -> pRefIdsInInventory.contains(pRefId)
    )) throw new RuntimeException("Uno de los productos enviados para borrar no se encuentra en el inventario seleccionado. %s"
      .formatted(productRefIds.stream().filter(pRefId -> !pRefIdsInInventory.contains(pRefId)).toList().toString())
    );

    List<String> refIdsOfExclusiveProducts = productInvRepository.findReferenceIdsExclusiveToInventory(invId, accountId).stream().map(
      uuid -> uuid.toString()
    ).toList();
    if (!refIdsOfExclusiveProducts.isEmpty()) {
      String url = UriComponentsBuilder.fromUriString("http://api-products:8081/product/delete-by-ids/common-perm")
        .queryParam("ids", refIdsOfExclusiveProducts)
        .queryParam("invId", invId.toString())
        .queryParam("accountId", accountId.toString())
      .toUriString();
      makeRestRequest(url, HttpMethod.DELETE, null);
    }

    List<ProductInInvEntity> psInInvToDelete = productsInInv.stream().filter(
      pInInv -> productRefIds.contains(pInInv.getReferenceId())
    ).toList();
    productInvRepository.deleteAll(psInInvToDelete);
    return true;
  }

  @Override @Transactional
  public boolean delete(UUID id, UUID accountId) {
    InventoryEntity inv = repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    );
    
    String baseUrlToUsers = "http://api-users:8082/account/remove-inventory";
    String completeUrlToUsers = UriComponentsBuilder.fromUriString(baseUrlToUsers)
      .queryParam("accountId", inv.getAccountId().toString())
      .queryParam("invId", inv.getId().toString())
    .toUriString();
    makeRestRequest(completeUrlToUsers, HttpMethod.DELETE, null);
    
    List<UUID> refIdsToDelete = productInvRepository.findReferenceIdsExclusiveToInventory(id, accountId);
    String baseUrlToProducts = "http://api-products:8081/product/delete-by-ids";
    String completeUrlToProducts = UriComponentsBuilder.fromUriString(baseUrlToProducts)
      .queryParam("ids", refIdsToDelete.toArray())
      .queryParam("accountId", accountId.toString())
    .toUriString();
    makeRestRequest(completeUrlToProducts, HttpMethod.DELETE, null);
    
    productInvRepository.deleteAll(productInvRepository.findByInventory(inv));
    repository.deleteById(id);
    return true;
  }

  @Override @Transactional
  public boolean deleteByAccountId(UUID id) {
    List<InventoryEntity> invs = repository.findByAccountId(id);
    repository.deleteAll(invs);
    return true;
  }
}
