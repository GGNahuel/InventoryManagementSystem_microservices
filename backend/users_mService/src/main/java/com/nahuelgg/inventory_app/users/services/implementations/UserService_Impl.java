package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;
  private final HttpGraphQlClient client;

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

    if (repository.findByNameAndAssociatedAccountId(updatedUser.getName(), userInDB.getAssociatedAccount().getId()).isPresent())
      throw new InvalidValueException("Ya existe un usuario con ese nombre asociado a esta cuenta");

    UserEntity newUser = dtoMappers.mapUser(updatedUser, userInDB.getAssociatedAccount());
    newUser.setInventoryPerms(userInDB.getInventoryPerms()); // la idea es que solo se editen los permisos con sus respectivos métodos

    return entityMappers.mapUser(repository.save(newUser));
  }

  @Override @Transactional
  public UserDTO assignNewPerms(PermissionsForInventoryDTO permission, UUID userId) throws JsonProcessingException {
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

    client.document("""
      mutation ($user: UserInputFromUSERS_MS!, $invId: ID!) {
        addUser(user: $user, invId: $invId)
      }""").variables(Map.of(
        "user", objectMapper.writeValueAsString(user),
        "invId", permission.getIdOfInventoryReferenced()
      )
    ).retrieve("addUser").toEntity(Boolean.class);

    return entityMappers.mapUser(repository.save(user));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id));

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    );

    client.document("""
      mutation {
        removeUser(
          userId: %s, accountId: %s
        )
      }
    """.formatted(
      id.toString(), user.getAssociatedAccount().getId().toString()
    )).retrieve("removeUser").toEntity(Boolean.class);

    repository.deleteById(id);
  }
}
