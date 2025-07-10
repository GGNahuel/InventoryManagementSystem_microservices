package com.nahuelgg.inventory_app.products.utilities;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("test")
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {
  private final ProductRepository productRepository;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    UUID acc1Id = UUID.fromString("acc00000-0000-0000-0000-000000000001");
    UUID acc2Id = UUID.fromString("acc00000-0000-0000-0000-000000000002");

    List<ProductEntity> productsToSave = List.of(
      ProductEntity.builder()
        .name("ProductA")
        .brand("brand1")
        .accountId(acc1Id)
        .unitPrice(5.0)
      .build(),
      ProductEntity.builder()
        .name("ProductB")
        .accountId(acc1Id)
        .unitPrice(8.5)
      .build(),
      ProductEntity.builder()
        .name("ProductA")
        .brand("brand2")
        .accountId(acc1Id)
        .unitPrice(7.0)
      .build(),

      ProductEntity.builder()
        .name("ProductA")
        .accountId(acc2Id)
        .unitPrice(6.0)
      .build(),
      ProductEntity.builder()
        .name("ProductB")
        .accountId(acc2Id)
        .unitPrice(7.0)
      .build()
    );

    productRepository.saveAll(productsToSave);
  }
}
