package com.nahuelgg.inventory_app.products.repositories;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ActiveProfiles("test")
public class RepositoriesTest {
  private final ProductRepository repository;

  @Autowired
  public RepositoriesTest(ProductRepository repository) {
    this.repository = repository;
  }

  private ProductEntity pr1, pr2, pr3, pr4;
  private UUID acc1ID = UUID.randomUUID();
  private UUID acc2ID = UUID.randomUUID();

  @BeforeEach
  void beforeEach() {
    repository.deleteAll();

    pr1 = ProductEntity.builder()
      .name("Ventilador")
      .brand("Marca 1")
      .unitPrice(80.0)
      .categories("cat1")
      .accountId(acc1ID)
    .build();
    pr2 = ProductEntity.builder()
      .name("Ventilador de techo")
      .brand("Marca 2")
      .unitPrice(115.0)
      .categories("cat1")
      .accountId(acc1ID)
    .build();
    pr3 = ProductEntity.builder()
      .name("Lampara")
      .brand("Marca 2")
      .unitPrice(15.0)
      .categories("cat1")
      .accountId(acc1ID)
    .build();
    pr4 = ProductEntity.builder()
      .name("Abrigo")
      .brand("Marca 3")
      .unitPrice(25.0)
      .categories("cat2")
      .accountId(acc2ID)
    .build();

    repository.saveAll(List.of(pr1, pr2, pr3, pr4));
  }

  @Test
  void productRepository_search_returnsExpected() {
    List<ProductEntity> expectedByName = List.of(pr1, pr2);
    List<ProductEntity> expectedByCategoryName = List.of(pr1, pr2, pr3);
    List<ProductEntity> expectedByAccount = List.of(pr4);

    assertIterableEquals(expectedByName, 
      repository.search(null, "ventilador", null, null, acc1ID));
    assertIterableEquals(expectedByCategoryName, 
      repository.search(null, null, null, List.of("cat1"), acc1ID));
    assertIterableEquals(expectedByAccount, 
      repository.search(null, null, null, null, acc2ID));
  }

  @Test
  void productRepository_findByAccount_returnsExpected() {
    List<ProductEntity> expected1 = List.of(pr1, pr2, pr3);
    List<ProductEntity> expected2 = List.of(pr4);

    assertIterableEquals(expected1, repository.findByAccountId(acc1ID));
    assertIterableEquals(expected2, repository.findByAccountId(acc2ID));
  }
}
