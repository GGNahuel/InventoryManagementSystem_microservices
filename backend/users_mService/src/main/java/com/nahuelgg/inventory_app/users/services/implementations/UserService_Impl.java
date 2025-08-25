package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nahuelgg.inventory_app.users.components.DTOMappers;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.exceptions.InvalidValueException;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.UserService;
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

    UserEntity result = repository.findById(id).orElse(null);

    return result != null ? entityMappers.mapUser(result) : null;
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
  public UserDTO managePerms(PermissionsForInventoryDTO input, UUID userId, UUID accountId) throws JsonProcessingException {
    checkFieldsHasContent(
      new Field("permiso", input),
      new Field("id de usuario", userId),
      new Field("id de la cuenta", accountId)
    );
    checkFieldsHasContent(
      new Field("lista de permisos", input.getPermissions()), 
      new Field("id inventario asociado", input.getIdOfInventoryReferenced())
    );

    UserEntity user = repository.findById(userId).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", userId.toString())
    );

    List<PermissionsForInventoryEntity> perms = user.getInventoryPerms();
    PermissionsForInventoryEntity mappedInput = dtoMappers.mapPerms(input);
    Optional<PermissionsForInventoryEntity> previousPerm = perms.stream().filter(
      permEntity -> permEntity.getInventoryReference().getInventoryIdReference().toString().equals(input.getIdOfInventoryReferenced())
    ).findFirst();

    if (previousPerm.isPresent()) {
      previousPerm.get().setPermissions(mappedInput.getPermissions());
      PermissionsForInventoryEntity permSaved = permsRepository.save(previousPerm.get());

      // remueve el permiso que se trajo de la base de datos anteriormente y lo vuelve a agregar a la lista del usuario pero con los cambios
      // Esto para incluirlo en el dto de retorno sin tener que hacer otro llamado a la base de datos.
      perms.removeIf(
        permEntity -> permEntity.getInventoryReference().equals(permSaved.getInventoryReference())
      );
      perms.add(permSaved);
    } else {
      perms.add(permsRepository.save(mappedInput));
    }
    
    // setea la nueva lista de permisos según los cambios realizados
    user.setInventoryPerms(perms);
  
    return entityMappers.mapUser(user);
  }

  @Override @Transactional
  public void deletePerm(UUID inventoryRef, UUID userId) {
    checkFieldsHasContent(new Field("id de inventario", inventoryRef), new Field("id del sub-usuario", userId));

    PermissionsForInventoryEntity permToDelete = permsRepository.findByInventoryReferenceIdAndUserId(inventoryRef, userId).orElseThrow(
      () -> new ResourceNotFoundException("permiso", "id de inventario o id de usuario", "invId: %s, userId: %s".formatted(
        inventoryRef.toString(), userId.toString()
      ))
    );

    permsRepository.delete(permToDelete);
  }

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
