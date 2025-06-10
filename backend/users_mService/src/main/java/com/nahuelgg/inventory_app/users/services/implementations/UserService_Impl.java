package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

@Service
public class UserService_Impl implements UserService {
  private final UserRepository repository;
  private final PermissionsForInventoryRepository permsRepository;
  private final DTOMappers dtoMappers;
  private final EntityMappers entityMappers = new EntityMappers();
  private final ObjectMapper objectMapper;

  public UserService_Impl(UserRepository repository, PermissionsForInventoryRepository permsRepository, DTOMappers dtoMappers, ObjectMapper objMapper) {
    this.repository = repository;
    this.permsRepository = permsRepository;
    this.dtoMappers = dtoMappers;
    this.objectMapper = objMapper;
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

    UserEntity newUser = dtoMappers.mapUser(updatedUser, userInDB.getAssociatedAccount());
    // la idea es que solo se editen los permisos con sus respectivos métodos
    newUser.setInventoryPerms(userInDB.getInventoryPerms());

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

    String userDTOtoString = objectMapper.writeValueAsString(entityMappers.mapUser(user));
    Map<String, String> requestBody = Map.of("query", """
      mutation {
        addUser(
          user: """ + userDTOtoString + """
          , invId: """ + permission.getIdOfInventoryReferenced() + """
        )
      }
    """);
    WebClient.create("http://api-inventory/graphql")
      .post()
      .uri("/")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(requestBody)
      .retrieve()
      .bodyToMono(Boolean.class)
    .block();

    return entityMappers.mapUser(repository.save(user));
  }

  @Override @Transactional
  public void delete(UUID id) {
    checkFieldsHasContent(new Field("id", id));

    UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    );
    
    Map<String, String> requestBody = Map.of("query", """
      mutation {
        removeUser(
          userId: """ + id.toString() + """
          , accountId: """ + user.getAssociatedAccount().getId().toString() + """
        )
      }
    """);
    WebClient.create("http://api-inventory/graphql")
      .post()
      .uri("/")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(requestBody)
      .retrieve()
      .bodyToMono(Boolean.class)
    .block();

    repository.deleteById(id);
  }

  @Override
  public boolean checkUserIsAdmin() {
    /* ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    UserSessionDTO userLogged = (UserSessionDTO) session.getAttribute(Constants.userSessionAttr);
    return userLogged != null && userLogged.getIsAdmin(); */
    return true;
  }

  @Override
  public void loginAsUser(UUID id, String password) {
    checkFieldsHasContent(new Field("id", id), new Field("contraseña", password));

    /* UserEntity user = repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("usuario", "id", id.toString())
    ); */

/*     ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    session.setAttribute(Constants.userSessionAttr, entityMappers.mapUser_session(user)); */
  }

  @Override
  public void logoutUser() {
    /* ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    session.setAttribute(Constants.userSessionAttr, null); */
  }
}
