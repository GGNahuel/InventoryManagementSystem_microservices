package com.nahuelgg.inventory_app.users.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.users.entities.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>{
  
}
