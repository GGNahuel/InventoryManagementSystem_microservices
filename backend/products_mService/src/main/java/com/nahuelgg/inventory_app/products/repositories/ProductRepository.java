package com.nahuelgg.inventory_app.products.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>{
  @Query("select p from ProductEntity p where p.categories.name = ?1")
  List<ProductEntity> findByCategoryName(String category);

  /*
    Por cada producto que revise se fijará cada una de las condiciones.
    Cada condición devolverá true si coincide con el parámetro o el valor de este es nulo,
    lo que por ejemplo si no se envía ningún parámetro la query devolverá todos los productos.
    ya que todas las condiciones se cumplieron
  */
  @Query("select p from ProductEntity p where" + 
    "(p.brand = ?1 or ?1 is null) and" +
    "(p.name = ?2 or ?2is null) and" +
    "(p.model like %?3% or ?3 is null)"
  )
  List<ProductEntity> findByBrandNameAndModel(String brand, String name, String model);
}
