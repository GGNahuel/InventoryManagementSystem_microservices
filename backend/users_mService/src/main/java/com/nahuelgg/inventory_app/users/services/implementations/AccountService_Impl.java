package com.nahuelgg.inventory_app.users.services.implementations;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.nahuelgg.inventory_app.users.components.DTOMappers;
import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.AccountRegistrationDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.TokenDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.dtos.UserRegistrationDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.exceptions.InvalidValueException;
import com.nahuelgg.inventory_app.users.exceptions.ResourceNotFoundException;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.AccountService;
import com.nahuelgg.inventory_app.users.services.AuthenticationService;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService_Impl implements AccountService {
  private final AccountRepository repository;
  private final UserRepository userRepository;
  private final InventoryRefRepository inventoryRefRepository;
  private final PermissionsForInventoryRepository permsRepository;
  
  private final DTOMappers dtoMappers;
  private final EntityMappers entityMappers = new EntityMappers();
  private final BCryptPasswordEncoder encoder;
  private final AuthenticationService authenticationService;
  
  private final RestTemplate restTemplate;
  private final HttpGraphQlClient client;


  @Override @Transactional(readOnly = true)
  public List<AccountDTO> getAll() {
    return repository.findAll().stream().map(
      acc -> entityMappers.mapAccount(acc)
    ).toList();
  }

  @Override @Transactional(readOnly = true)
  public AccountDTO getById(UUID id) {
    checkFieldsHasContent(new Field("id de cuenta", id));

    return entityMappers.mapAccount(repository.findById(id).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "id", id.toString())
    ));
  }

  @Override @Transactional
  public AccountDTO create(AccountRegistrationDTO info) {
    checkFieldsHasContent(new Field("información de creación de la cuenta", info));
    String username = info.getUsername();
    String password = info.getPassword();
    String passwordRepeated = info.getPasswordRepeated();
    String adminPassword = info.getAdminPassword();
    String adminPasswordRepeated = info.getAdminPasswordRepeated();

    checkFieldsHasContent(
      new Field("nombre de usuario", username), new Field("contraseña", password), new Field("contraseña de admin", adminPassword),
      new Field("repetición de contraseña", passwordRepeated), new Field("repetición de contraseña de admin", adminPasswordRepeated)
    );
    if (!password.equals(passwordRepeated) || !adminPassword.equals(adminPasswordRepeated))
      throw new InvalidValueException("Las contraseñas no coinciden con sus respectivas repeticiones");

    AccountEntity accountSaved = repository.save(AccountEntity.builder()
      .username(username)
      .nickName(info.getNickName())
      .password(encoder.encode(password))
    .build());

    UserEntity adminUser = userRepository.save(UserEntity.builder()
      .name("admin")
      .password(encoder.encode(adminPassword))
      .role("admin")
      .isAdmin(true)
      .associatedAccount(accountSaved)
    .build());

    return entityMappers.mapAccount(accountSaved.toBuilder().users(List.of(adminUser)).build());
  }

  @Override @Transactional
  public UserDTO addUser(UUID accountId, UserRegistrationDTO info) {
    checkFieldsHasContent(new Field("usuario a agregar", info));
    checkFieldsHasContent(
      new Field("nombre del usuario", info.getName()), new Field("rol/puesto del usuario", info.getRole()),
      new Field("id de cuenta a asociar", accountId),
      new Field("contraseña para el usuario", info.getPassword()), new Field("repetición de contraseña", info.getPasswordRepeated())
    );
    if (info.getInventoryPerms() != null && !info.getInventoryPerms().isEmpty()) {
      for (PermissionsForInventoryDTO perm : info.getInventoryPerms()) {
        checkFieldsHasContent(
          new Field("inventario de referemcia", perm.getIdOfInventoryReferenced()),
          new Field("permisos", perm.getPermissions())
        );
      }
    }

    if (!info.getPassword().equals(info.getPasswordRepeated()))
      throw new InvalidValueException("Las contraseñas para el nuevo usuario no coinciden");

    if (userRepository.findByNameAndAssociatedAccountId(info.getName(), accountId).isPresent())
      throw new InvalidValueException("Ya existe un usuario con ese nombre asociado a esta cuenta");

    AccountEntity parentAccount = repository.findById(accountId).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "id", accountId.toString())
    );

    List<PermissionsForInventoryEntity> permsEntities = new ArrayList<>();
    if (info.getInventoryPerms() != null) {
      for (int i = 0; i < info.getInventoryPerms().size(); i++) {
        PermissionsForInventoryDTO permsDto = info.getInventoryPerms().get(i);

        permsEntities.add(permsRepository.save(dtoMappers.mapPerms(permsDto)));
      }
    }

    UserEntity savedUser = userRepository.save(UserEntity.builder()
      .name(info.getName())
      .role(info.getRole())
      .password(encoder.encode(info.getPassword()))
      .associatedAccount(parentAccount)
      .isAdmin(false)
      .inventoryPerms(permsEntities)
    .build());

    return entityMappers.mapUser(savedUser);
  }

  @Override @Transactional
  public void assignInventory(UUID accountId, UUID inventoryRefId) {
    checkFieldsHasContent(new Field("id de cuenta", accountId), new Field("id referenciada de inventario", inventoryRefId));

    AccountEntity account = repository.findById(accountId).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "id", accountId.toString())
    );

    List<InventoryRefEntity> inventoriesReferences = account.getInventoriesReferences() == null ? new ArrayList<>() : account.getInventoriesReferences();
    inventoriesReferences.add(
      inventoryRefRepository.save(InventoryRefEntity.builder().inventoryIdReference(inventoryRefId).build())
    );
  }

  @Override @Transactional
  public void removeInventoryAssigned(UUID accountId, UUID inventoryRefId) {
    checkFieldsHasContent(new Field("id de cuenta", accountId), new Field("id referenciada de inventario", inventoryRefId));

    AccountEntity account = repository.findById(accountId).orElseThrow(
      () -> new ResourceNotFoundException("cuenta", "id", accountId.toString())
    );
    inventoryRefRepository.findByInventoryIdReference(inventoryRefId).orElseThrow(
      () -> new ResourceNotFoundException("entidad de referencia a inventario", "id de referencia", inventoryRefId.toString())
    );

    List<InventoryRefEntity> inventoriesReferences = account.getInventoriesReferences().stream().filter(
      invRefEntity -> !invRefEntity.getInventoryIdReference().equals(inventoryRefId)
    ).collect(Collectors.toCollection(ArrayList::new));
    List<PermissionsForInventoryEntity> permsAssociatedWithInventoryRef = permsRepository.findByReferencedInventoryId(inventoryRefId);
    permsRepository.deleteAll(permsAssociatedWithInventoryRef);

    account.setInventoriesReferences(inventoriesReferences);
    repository.save(account);
  }

  @Override @Transactional
  public TokenDTO delete(UUID accountId) {
    checkFieldsHasContent(new Field("id de cuenta", accountId));

    boolean accountWithIdExists = repository.findById(accountId).isPresent();
    if (accountWithIdExists) 
      throw new ResourceNotFoundException("cuenta", "id", accountId.toString());

    Boolean inventoryRequestWasSuccess = client.document("""
      mutation {
        deleteByAccountId(
          id: "%s"
        )
      }
    """.formatted(accountId.toString())).retrieve("deleteByAccountId").toEntity(Boolean.class).block();

    if (inventoryRequestWasSuccess == null || !inventoryRequestWasSuccess) 
      throw new RuntimeException("El borrado de inventarios asociados no se ha podido realizar, operación cancelada");

    restTemplate.delete("http://api-products:8081/product/delete-by-account?id=" + accountId.toString());
    repository.deleteById(accountId);

    return authenticationService.logout();
  }
}
