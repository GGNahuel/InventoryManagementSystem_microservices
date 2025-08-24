package com.nahuelgg.inventory_app.inventories.services.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.EditProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaInputs.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.schemaOutputs.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;
import com.nahuelgg.inventory_app.inventories.utilities.Mappers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService_Impl implements InventoryService {
  private final InventoryRepository repository;
  private final ProductInInvRepository productInvRepository;
  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper;
  private final Mappers mappers = new Mappers();

  private HttpHeaders setTokenToOtherServicesRequests() {
    HttpHeaders header = new HttpHeaders();

    ServletRequestAttributes attributes =
      (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes == null) 
      throw new RuntimeException("Error al intentar obtener los atributos de la request");

    HttpServletRequest request = attributes.getRequest();
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer "))
      throw new RuntimeException("Error al obtener el header de autorización");

    header.setBearerAuth(authHeader.substring(7));
    return header;
  }

  private ResponseDTO makeRestRequest(String url, HttpMethod method, Object optionalBody) {
    ResponseEntity<ResponseDTO> response;
    try {
      HttpEntity<Object> entity = optionalBody != null ? 
        new HttpEntity<Object>(optionalBody, setTokenToOtherServicesRequests()) : 
        new HttpEntity<>(setTokenToOtherServicesRequests());

      response = restTemplate.exchange(url, method, entity, ResponseDTO.class);
      System.out.println(response.toString());
      if (!response.getStatusCode().is2xxSuccessful())
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
    List<ProductFromProductsMSDTO> responseList = objectMapper.convertValue(
      responseDto.getData(),
      new TypeReference<List<ProductFromProductsMSDTO>>() {}
    );

    sincroniceProductsBetweenServices(productsId, responseList.stream().map(ProductFromProductsMSDTO::getId).toList());

    return responseList;
  }

  // al buscar por ids en el microservicio de productos, si llega una id que no corresponde a ningún producto guardado no se devuelve nada para esa entidad,
  // lo que significa que probablemente el producto haya sido eliminado directamente desde ese microservicio. Por lo que el servicio de inventario tendría
  // eliminar también los productos con esa referencia.
  private void sincroniceProductsBetweenServices(List<String> referenceIdsInInv, List<String> referenceIdsObtained) {
    List<String> idsOfProductInInvToDelete = referenceIdsInInv.stream().filter(
      refIdInInv -> !referenceIdsObtained.contains(refIdInInv)
    ).toList();

    for (String refId : idsOfProductInInvToDelete) {
      List<ProductInInvEntity> productsToDelete = productInvRepository.findByReferenceId(UUID.fromString(refId));

      productInvRepository.deleteAll(productsToDelete);
    }
  }

  @Override @Transactional(readOnly = true)
  public InventoryDTO getById(UUID id) {
    InventoryEntity inv = repository.findById(id).orElse(null);
    if (inv == null) return null;

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
    List<ProductFromProductsMSDTO> resultsOfProducts = objectMapper.convertValue(
      makeRestRequest(completeUrl, HttpMethod.GET, null).getData(),
      new TypeReference<List<ProductFromProductsMSDTO>>() {}
    );

    // busca inventarios en base a la lista de ids de referencia obtenidas en la solicitud al servicio de productos, 
    // al resultado lo mapea a DTO
    List<InventoryDTO> inventoriesWithThoseProducts = repository.searchByProductRefId(
      resultsOfProducts.stream().map(
        p -> UUID.fromString(p.getId())
      ).toList()
    ).stream().map(
      i -> mappers.mapInvEntity(i, resultsOfProducts)
    ).toList();

    // modifica cada DTO de la lista, filtrando los productos a aquellos que contengan alguna de las ids de referencia obtenidas
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
    if (repository.existsByNameAndAccountId(name, accountId))
      throw new RuntimeException("Ya existe un inventario con ese nombre en la cuenta");

    InventoryEntity inv = repository.save(InventoryEntity.builder().name(name).accountId(accountId).build());

    String baseUrl = "http://api-users:8082/account/add-inventory";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("accountId", accountId.toString())
      .queryParam("invRefId", inv.getId().toString())
    .toUriString();
    makeRestRequest(completeUrl, HttpMethod.PUT, null);

    return mappers.mapInvEntity(inv, List.of());
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
  public ProductInInvDTO addProduct(ProductInputDTO productInput, UUID invId, UUID accountId) {
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

    // se fijará si la referencia de ese producto está únicamente en el inventario seleccionado, si es el caso llama al endpoint
    // que edita el producto de referencia, caso contrario creara uno nuevo
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

  @Override @Transactional
  public boolean copyProducts(List<ProductToCopyDTO> products, UUID idTo) {
    InventoryEntity invTo = repository.findById(idTo).orElseThrow(
      () -> new RuntimeException("")
    );

    List<ProductInInvEntity> newList = invTo.getProducts();
    // en base a la lista de los productos en el input arma las nuevas entidades al inventario seleccionado
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

    // Se revisa si las ids de referencia a borrar pertenecen solo al inventario seleccionado, si es el caso se borran esos productos en su BDD
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

    // Luego selecciona aquellos productos en inventario, en la base de datos de este servicio, que correspondan al inventario seleccionado y 
    // a las ids de referencia enviadas. Se lo hace a través de filtrado para no consultar nuevamente a la base de datos
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
      .queryParam("invRefId", inv.getId().toString())
    .toUriString();
    makeRestRequest(completeUrlToUsers, HttpMethod.PUT, null);
    
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
    List<InventoryEntity> inventories = repository.findByAccountId(id);
    repository.deleteAll(inventories);
    return true;
  }
}
