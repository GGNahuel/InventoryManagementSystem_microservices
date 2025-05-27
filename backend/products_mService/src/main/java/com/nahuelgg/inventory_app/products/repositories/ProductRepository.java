package com.nahuelgg.inventory_app.products.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.products.Entities.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>{
  @Query("select p from ProductEntity p where p.categories.name = ?1")
  List<ProductEntity> findByCategoryName(String category);

  List<ProductEntity> findByBrand(String brand);

  List<ProductEntity> findByName(String name);
}
