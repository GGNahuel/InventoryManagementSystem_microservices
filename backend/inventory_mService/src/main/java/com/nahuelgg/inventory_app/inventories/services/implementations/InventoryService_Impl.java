package com.nahuelgg.inventory_app.inventories.services.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nahuelgg.inventory_app.inventories.dtos.AccountFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.InventoryDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductFromProductsMSDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInInvDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductInputDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ProductToCopyDTO;
import com.nahuelgg.inventory_app.inventories.dtos.ResponseDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO;
import com.nahuelgg.inventory_app.inventories.entities.InventoryEntity;
import com.nahuelgg.inventory_app.inventories.entities.ProductInInvEntity;
import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import com.nahuelgg.inventory_app.inventories.repositories.InventoryRepository;
import com.nahuelgg.inventory_app.inventories.repositories.ProductInInvRepository;
import com.nahuelgg.inventory_app.inventories.repositories.UserReferenceRepository;
import com.nahuelgg.inventory_app.inventories.services.InventoryService;
import com.nahuelgg.inventory_app.inventories.utitlities.Mappers;

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

  private List<ProductFromProductsMSDTO> getProductsFromMS(InventoryEntity inv) {
    List<String> productsId = inv.getProducts().stream().map(
      pInInvEntity -> pInInvEntity.getReferenceId().toString()
    ).toList();

    String baseUrl = "http://api_products:8081/product/ids";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("list", productsId.toArray())
    .toUriString();
  
    return (List<ProductFromProductsMSDTO>) restTemplate.getForObject(completeUrl, ResponseDTO.class).getData();
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
  public InventoryDTO getByNameAndAccount(String name, UUID accountId) {
    InventoryEntity inv = repository.findByNameAndAccountId(name, accountId).orElseThrow(
      () -> new RuntimeException("")
    );
    List<ProductFromProductsMSDTO> productsFromMS = getProductsFromMS(inv);

    return mappers.mapInvEntity(inv, productsFromMS);
  }

  @Override @Transactional(readOnly = true)
  public List<InventoryDTO> searchProductsInInventories(
    String name, String brand, String model, List<String> categories, UUID accountId
  ) {
    String baseUrl = "http://api_products:8081/product/search";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("name", name)
      .queryParam("brand", brand)
      .queryParam("model", model)
      .queryParam("categoryNames", categories.toArray())
      .queryParam("accountId", accountId.toString())
    .toUriString(); 
    List<ProductFromProductsMSDTO> resultsOfProducts = (List<ProductFromProductsMSDTO>) restTemplate.getForObject(completeUrl, ResponseDTO.class).getData();

    return repository.searchByProductRefId(
      resultsOfProducts.stream().map(
        p -> UUID.fromString(p.getId())
      ).toList()
    ).stream().map(
      i -> mappers.mapInvEntity(i, resultsOfProducts)
    ).toList();
  }

  // mutations
  @Override @Transactional
  public InventoryDTO create(String name, UUID accountId) {
    InventoryEntity inv = repository.save(InventoryEntity.builder().name(name).build());

    String baseUrl = "http://api_users:8082/account/add_inventory";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("accountId", accountId.toString())
      .queryParam("invId", inv.getId().toString())
    .toUriString();
    AccountFromUsersMSDTO account = (AccountFromUsersMSDTO) restTemplate.getForObject(completeUrl, ResponseDTO.class).getData();

    inv.setAccountId(UUID.fromString(account.getId()));
    inv.setUsers(account.getUsers().stream().map(
      u -> userRefRepository.findByReferenceId(UUID.fromString(u.getId())).orElse(
        userRefRepository.save(
          UserReferenceEntity.builder().referenceId(UUID.fromString(u.getId())).build()
        )
      )
    ).toList());

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
  public boolean addUser(UserFromUsersMSDTO user, UUID invId) {
    InventoryEntity inv = repository.findById(invId).orElseThrow(
      () -> new RuntimeException("")
    );
    List<UserReferenceEntity> userRefs = inv.getUsers();
    userRefs.add(userRefRepository.save(
      UserReferenceEntity.builder().referenceId(UUID.fromString(user.getId())).build()
    ));
    inv.setUsers(userRefs);

    repository.save(inv);
    return true;
  }

  @Override @Transactional
  public ProductInInvDTO addProduct(ProductInputDTO productInput, UUID invId) {
    String baseUrl = "http://api_products:8081/product/";
    ProductFromProductsMSDTO productCreated = (ProductFromProductsMSDTO) restTemplate.postForObject(
      baseUrl, 
      mappers.mapProductInput(productInput), 
      null
    );
    
    InventoryEntity inv = repository.findById(invId).orElseThrow(
      () -> new RuntimeException("")
    );

    ProductInInvEntity newProductInv = productInvRepository.save(ProductInInvEntity.builder()
      .referenceId(UUID.fromString(productCreated.getId()))
      .stock(productInput.getStock())
      .isAvailable(productInput.getStock() > 0)
      .inventory(inv)
    .build());
    List<ProductInInvEntity> newListProductInInv = inv.getProducts();
    newListProductInInv.add(newProductInv);

    inv.setProducts(newListProductInInv);
    repository.save(inv);
    return mappers.mapProductsFromMSToDTO(productCreated, newProductInv);
  }

  @Override @Transactional
  public boolean copyProducts(List<ProductToCopyDTO> products, UUID idTo) {
    InventoryEntity invTo = repository.findById(idTo).orElseThrow(
      () -> new RuntimeException("")
    );

    List<ProductInInvEntity> newList = invTo.getProducts();
    for (ProductToCopyDTO p : products) {
      if (!newList.stream().filter(pInv -> p.getId().equals(pInv.getReferenceId().toString())).findFirst().isPresent()) {
        newList.add(productInvRepository.save(ProductInInvEntity.builder()
          .referenceId(UUID.fromString(p.getId()))
          .stock(p.getStock())
          .isAvailable(p.getStock() > 0)
          .inventory(invTo)
        .build()));
      }
    }

    invTo.setProducts(newList);
    repository.save(invTo);
    return true;
  }

  @Override @Transactional
  public boolean editStockOfProduct(int relativeNewStock, UUID productRefId, UUID invId) {
    ProductInInvEntity p = productInvRepository.findByReferenceIdAndInventoryId(productRefId, invId).orElseThrow(
      () -> new RuntimeException("")
    );
    int newStock = p.getStock() + relativeNewStock;
    p.setStock(newStock);
    p.setIsAvailable(newStock > 0);
    return true;
  }

  @Override @Transactional
  public boolean delete(UUID id) {
    InventoryEntity inv = repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    );
    repository.deleteById(id);

    String baseUrl = "http://api_users/account/remove_inventory";
    String completeUrl = UriComponentsBuilder.fromUriString(baseUrl)
      .queryParam("accountId", inv.getAccountId().toString()) // esto lo tendría que extraer del JWT
      .queryParam("invId", inv.getId().toString())
    .toUriString();
    restTemplate.delete(completeUrl);

    return true;
  }
}
