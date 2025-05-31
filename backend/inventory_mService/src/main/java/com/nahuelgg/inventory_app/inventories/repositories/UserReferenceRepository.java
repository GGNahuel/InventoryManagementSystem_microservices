package com.nahuelgg.inventory_app.inventories.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nahuelgg.inventory_app.inventories.entities.UserReferenceEntity;
import java.util.List;

@Repository
public interface UserReferenceRepository extends JpaRepository<UserReferenceEntity, UUID> {
  List<UserReferenceEntity> findByReferenceId(UUID referenceId);
}
