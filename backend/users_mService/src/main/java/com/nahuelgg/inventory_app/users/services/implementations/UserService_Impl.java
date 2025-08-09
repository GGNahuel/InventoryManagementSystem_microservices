package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.exceptions.InvalidValueException;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.UserService;
import com.nahuelgg.inventory_app.users.utilities.DTOMappers;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService_Impl implements UserService {
  private final UserRepository repository;
  private final PermissionsForInventoryRepository permsRepository;
  private final DTOMappers dtoMappers;
  private final EntityMappers entityMappers = new EntityMappers();

  @Override @Transactional(readOnly = true)
  public UserDTO getById(UUID id, UUID accountId) {
    checkFieldsHasContent(new Field("id", id), new Field("id de la cuenta", accountId));

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    );

    return entityMappers.mapUser(user);
  }

  @Override @Transactional
  public UserDTO edit(UserDTO updatedUser, UUID accountId) {
    checkFieldsHasContent(new Field("usuario actualizado", updatedUser), new Field("id de cuenta", accountId));
    checkFieldsHasContent(
      new Field("id", updatedUser.getId()),
      new Field("nombre del usuario", updatedUser.getName()),
      new Field("rol/puesto del usuario", updatedUser.getRole())
    );

    UserEntity userInDB = repository.findById(UUID.fromString(updatedUser.getId())).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", updatedUser.getId())
    );

    if (repository.findByNameAndAssociatedAccountId(updatedUser.getName(), userInDB.getAssociatedAccount().getId()).isPresent())
      throw new InvalidValueException("Ya existe un usuario con ese nombre asociado a esta cuenta");

    // la idea es que solo se editen los permisos en su respectivo método, por lo que se ignoran los permisos que lleguen en el dto
    UserEntity newUser = userInDB.toBuilder()
      .name(updatedUser.getName())
      .role(updatedUser.getRole())
    .build();

    return entityMappers.mapUser(repository.save(newUser));
  }

  @Override @Transactional
  public UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId, UUID accountId) throws JsonProcessingException {
    checkFieldsHasContent(new Field("permiso", permission), new Field("id de usuario", userId), new Field("id de la cuenta", accountId));
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

  // TODO: agregar para eliminar y editar permisos

  @Override @Transactional
  public void delete(UUID id, UUID accountId) {
    checkFieldsHasContent(new Field("id", id), new Field("id de la cuenta", accountId));

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    );

    if (user.getIsAdmin()) 
      throw new InvalidValueException("No se puede eliminar un sub-usuario admin.");

    repository.deleteById(id);
  }
}
