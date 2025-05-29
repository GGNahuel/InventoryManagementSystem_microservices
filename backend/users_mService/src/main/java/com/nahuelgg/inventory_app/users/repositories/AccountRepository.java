package com.nahuelgg.inventory_app.users.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID>{
  Optional<AccountEntity> findByUsername(String username);
}
