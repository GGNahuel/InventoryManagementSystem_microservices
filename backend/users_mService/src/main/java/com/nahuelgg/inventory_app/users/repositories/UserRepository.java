package com.nahuelgg.inventory_app.users.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.UserEntity;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>{
  @Query("select u from users u where u.name = ?1 and u.associatedAccount.id = ?2")
  Optional<UserEntity> findByNameAndAssociatedAccountId(String name, UUID accountId);
}
