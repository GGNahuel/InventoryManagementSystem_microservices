package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.*;

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
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
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
    checkFieldsHasContent(new Field("id", id));

    return entityMappers.mapUser(repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    ));
  }

  @Override @Transactional
  public UserDTO edit(UserDTO updatedUser) {
    checkFieldsHasContent(new Field("usuario actualizado", updatedUser));
    checkFieldsHasContent(
      new Field("id", updatedUser.getId()),
      new Field("nombre del usuario", updatedUser.getName()),
      new Field("rol/puesto del usuario", updatedUser.getRole())
    );

    UserEntity userInDB = repository.findById(UUID.fromString(updatedUser.getId())).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", updatedUser.getId())
    );

    UserEntity newUser = dtoMappers.mapUser(updatedUser);
    // la idea es que solo se editen los permisos con sus respectivos métodos
    newUser.setInventoryPerms(userInDB.getInventoryPerms());

    return entityMappers.mapUser(repository.save(newUser));
  }

  @Override @Transactional
  public UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId) {
    checkFieldsHasContent(new Field("permiso", permission), new Field("id de usuario", userId));
    checkFieldsHasContent(
      new Field("lista de permisos", permission.getPermissions()), 
      new Field("id inventario asociado", permission.getIdOfInventoryReferenced())
    );

    UserEntity user = repository.findById(userId).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", userId.toString())
    );

    List<PermissionsForInventoryEntity> perms = user.getInventoryPerms();
    PermissionsForInventoryEntity newPerm = permsRepository.save(dtoMappers.mapPerms(permission));
    perms.add(newPerm);
    user.setInventoryPerms(perms);

    return entityMappers.mapUser(repository.save(user));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id));

    repository.deleteById(id);
  }

  @Override
  public void loginAsUser(UUID id, String password) {
    checkFieldsHasContent(new Field("id", id), new Field("contraseña", password));

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
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
