package com.nahuelgg.inventory_app.users.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID>{
  List<AccountEntity> findByUsername(String username);
}
