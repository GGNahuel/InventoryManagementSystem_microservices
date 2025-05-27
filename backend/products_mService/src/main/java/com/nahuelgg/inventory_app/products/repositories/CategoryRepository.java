package com.nahuelgg.inventory_app.products.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.products.Entities.CategoryEntity;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
  List<CategoryEntity> findByName(String name);
}
