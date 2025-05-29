package com.nahuelgg.inventory_app.users.services.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nahuelgg.inventory_app.users.dtos.AccountDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.dtos.UserDTO;
import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;
import com.nahuelgg.inventory_app.users.services.AccountService;
import com.nahuelgg.inventory_app.users.utilities.DTOMappers;
import com.nahuelgg.inventory_app.users.utilities.EntityMappers;

import jakarta.servlet.http.HttpSession;

@Service
public class AccountService_Impl implements AccountService{
  private final AccountRepository repository;
  private final UserRepository userRepository;
  private final InventoryRefRepository inventoryRefRepository;
  private final PermissionsForInventoryRepository permsRepository;
  private final DTOMappers dtoMappers;
  private final EntityMappers entityMappers = new EntityMappers();

  public AccountService_Impl(
    AccountRepository repository, UserRepository userRepository, 
    InventoryRefRepository inventoryRefRepository, PermissionsForInventoryRepository permsRepository,
    DTOMappers dtoMappers
  ) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.inventoryRefRepository = inventoryRefRepository;
    this.permsRepository = permsRepository;
    this.dtoMappers = dtoMappers;
  }

  @Override @Transactional(readOnly = true)
  public AccountDTO getById(UUID id) {
    return entityMappers.mapAccount(repository.findById(id).orElseThrow(
      () -> new RuntimeException("")
    ));
  }

  @Override @Transactional
  public AccountDTO create(String username, String password, String passwordRepeated, String adminPassword, String adminPasswordRepeated) {
    if (!password.equals(passwordRepeated) || !adminPassword.equals(adminPasswordRepeated))
      throw new RuntimeException("");

    UserEntity adminUser = userRepository.save(UserEntity.builder()
      .name("admin")
      .password(new BCryptPasswordEncoder().encode(adminPassword))
      .role("admin")
      .isAdmin(true)
    .build());

    AccountEntity accountToCreate = AccountEntity.builder()
      .username(username)
      .password(new BCryptPasswordEncoder().encode(password))
      .users(List.of(adminUser))
    .build();

    return entityMappers.mapAccount(repository.save(accountToCreate));
  }

  @Override @Transactional
  public UserDTO addUser(UserDTO user, UUID accountId, String passwordForNewUser, String passwordRepeated) {
    if (!passwordForNewUser.equals(passwordRepeated))
      throw new RuntimeException("");

    AccountEntity parentAccount = repository.findById(accountId).orElseThrow(
      () -> new RuntimeException("")
    );

    UserEntity newUser = dtoMappers.mapUser(user);
    List<PermissionsForInventoryEntity> permsEntities = new ArrayList<>();
    for (int i = 0; i < user.getInventoryPerms().size(); i++) {
      PermissionsForInventoryDTO permsDto = user.getInventoryPerms().get(i);

      permsEntities.add(permsRepository.save(dtoMappers.mapPerms(permsDto)));
    }

    newUser.setInventoryPerms(permsEntities);
    newUser.setIsAdmin(false);
    newUser.setPassword(new BCryptPasswordEncoder().encode(passwordForNewUser));
    UserEntity savedUser = userRepository.save(newUser);

    parentAccount.getUsers().add(savedUser);
    repository.save(parentAccount);
    return entityMappers.mapUser(savedUser);
  }

  @Override @Transactional
  public void assignInventory(UUID accountId, String inventoryId) {
    AccountEntity account = repository.findById(accountId).orElseThrow(
      () -> new RuntimeException("")
    );

    List<InventoryRefEntity> inventoriesReferences = account.getInventoriesReferences() == null ? new ArrayList<>() : account.getInventoriesReferences();
    inventoriesReferences.add(
      inventoryRefRepository.save(InventoryRefEntity.builder().inventoryIdReference(inventoryId).build())
    );

    account.setInventoriesReferences(inventoriesReferences);
    repository.save(account);
  }

  @Override @Transactional
  public void removeInventoryAssigned(UUID accountId, String inventoryId) {
    AccountEntity account = repository.findById(accountId).orElseThrow(
      () -> new RuntimeException("")
    );
    inventoryRefRepository.findByInventoryIdReference(inventoryId).orElseThrow(
      () -> new RuntimeException("")
    );

    List<InventoryRefEntity> inventoriesReferences = account.getInventoriesReferences().stream().filter(
      invRefEntity -> invRefEntity.getInventoryIdReference() != inventoryId
    ).toList();
    List<PermissionsForInventoryEntity> permsAssociatedWithInventoryRef = permsRepository.findByReferencedInventoryId(inventoryId);
    for (int i = 0; i < permsAssociatedWithInventoryRef.size(); i++) {
      permsRepository.deleteById(permsAssociatedWithInventoryRef.get(i).getId());
    }

    account.setInventoriesReferences(inventoriesReferences);
    repository.save(account);
  }

  @Override @Transactional
  public void delete(UUID accountId) {
    repository.deleteById(accountId);

    // agregar llamado al microservicio de inventarios para la eliminación de los asociados
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    AccountEntity account = repository.findByUsername(username).orElseThrow(
      () -> new RuntimeException("")
    );

    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpSession session = attr.getRequest().getSession();
    session.setAttribute("loggedAccount", account);

    return new User(account.getUsername(), account.getPassword(), new ArrayList<GrantedAuthority>());
  }
}
