package com.nahuelgg.inventory_app.products.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>{
  /*
    Por cada producto que revise se fijará cada una de las condiciones.
    Cada condición devolverá true si coincide con el parámetro o el valor de este es nulo,
    lo que por ejemplo si no se envía ningún parámetro la query devolverá todos los productos (a excepción del parámetro de cuenta).
    ya que todas las condiciones se cumplieron
  */
  @Query("select p from product p join p.categories c where" + 
    "(?1 is null or lower(p.brand) like %?1%) and" +
    "(?2 is null or lower(p.name) like %?2%) and" +
    "(?3 is null or lower(p.model) like %?3%) and" +
    "(?4 is null or c.name in ?4) and" + 
    "(p.accountId = ?5)"
  )
  List<ProductEntity> search(String brand, String name, String model, List<String> categoryNames, UUID accountId);

  List<ProductEntity> findByAccountId(UUID accountId);
}
