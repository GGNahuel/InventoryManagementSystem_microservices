package com.nahuelgg.inventory_app.users.services.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.DTOMappers;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;

import jakarta.servlet.http.HttpSession;

@Service
public class UserService_Impl implements UserService {
  private final UserRepository repository;
  private final PermissionsForInventoryRepository permsRepository;
  private final DTOMappers dtoMappers;
  private final EntityMappers entityMappers = new EntityMappers();

  public UserService_Impl(UserRepository repository, PermissionsForInventoryRepository permsRepository, DTOMappers dtoMappers) {
    this.repository = repository;
    this.permsRepository = permsRepository;
    this.dtoMappers = dtoMappers;
  }

  @Override @Transactional(readOnly = true)
  public UserDTO getById(UUID id) {
    return entityMappers.mapUser(repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    ));
  }

  @Override @Transactional
  public UserDTO edit(UserDTO updatedUser) {
    UserEntity userInDB = repository.findById(UUID.fromString(updatedUser.getId())).orElseThrow(
      () -> new RuntimeException("")
    );

    UserEntity newUser = dtoMappers.mapUser(updatedUser);
    newUser.setInventoryPerms(userInDB.getInventoryPerms());

    return entityMappers.mapUser(repository.save(newUser));
  }

  @Override @Transactional
  public UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId) {
    UserEntity user = repository.findById(userId).orElseThrow(
      () -> new RuntimeException("")
    );

    List<PermissionsForInventoryEntity> perms = user.getInventoryPerms();
    PermissionsForInventoryEntity newPerm = permsRepository.save(dtoMappers.mapPerms(permission));
    perms.add(newPerm);
    user.setInventoryPerms(perms);

    return entityMappers.mapUser(repository.save(user));
  }

  @Override @Transactional
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  @Override
  public void loginAsUser(UUID id, String password, String passwordRepeated)  {
    if (!password.equals(passwordRepeated))
      throw new RuntimeException("asd");

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    );

    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    session.setAttribute("loggedUser", user);
  }

  @Override
  public void logoutUser() {
    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    session.setAttribute("loggedUser", null);
  }
}
