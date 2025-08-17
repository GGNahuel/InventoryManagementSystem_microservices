package com.nahuelgg.inventory_app.users.services;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.users.entities.AccountEntity;
import com.nahuelgg.inventory_app.users.entities.InventoryRefEntity;
import com.nahuelgg.inventory_app.users.entities.PermissionsForInventoryEntity;
import com.nahuelgg.inventory_app.users.entities.UserEntity;
import com.nahuelgg.inventory_app.users.repositories.AccountRepository;
import com.nahuelgg.inventory_app.users.repositories.InventoryRefRepository;
import com.nahuelgg.inventory_app.users.repositories.PermissionsForInventoryRepository;
import com.nahuelgg.inventory_app.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseFillerWithExampleData implements CommandLineRunner {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final InventoryRefRepository inventoryRefRepository;
  private final PermissionsForInventoryRepository permsRepository;
  private final BCryptPasswordEncoder encoder;

  @Override
  public void run(String... args) throws Exception {
    final UUID exampleAccountId = UUID.fromString("12341234-0000-0000-0000-112233445566acc1");
    final UUID inventoryRefId1 = UUID.fromString("12341234-0000-1000-0001-1122334455667788");
    final UUID inventoryRefId2 = UUID.fromString("12341234-0000-1000-0002-1122334455667788");
    
    if (accountRepository.findById(exampleAccountId).isPresent()) return;

    InventoryRefEntity inv1 = inventoryRefRepository.save(InventoryRefEntity.builder()
      .inventoryIdReference(inventoryRefId1)
    .build());
    InventoryRefEntity inv2 = inventoryRefRepository.save(InventoryRefEntity.builder()
      .inventoryIdReference(inventoryRefId2)
    .build());

    AccountEntity acc = accountRepository.save(AccountEntity.builder()
      .id(exampleAccountId)
      .username("farmaciasHealth")
      .nickName("Farmacias Health")
      .password(encoder.encode("accountPassword"))
      .inventoriesReferences(List.of(inv1, inv2))
    .build());

    userRepository.save(UserEntity.builder()
      .name("admin")
      .password(encoder.encode("adminPassword"))
      .role("Administrador general")
      .isAdmin(true)
      .associatedAccount(acc)
    .build());

    // Creación de permisos y asociación al sub-usuario que corresponda
    // Gerente general
    PermissionsForInventoryEntity perm1ForGeneralManager = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("addProducts,editProducts,editProductReferences,deleteProducts,deleteProductReferences,editInventory")
      .inventoryReference(inv1)
    .build());
    PermissionsForInventoryEntity perm2ForGeneralManager = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("addProducts,editProducts,editProductReferences,deleteProducts,deleteProductReferences,editInventory")
      .inventoryReference(inv2)
    .build());
    userRepository.save(UserEntity.builder()
      .name("gerenteGeneral")
      .password(encoder.encode("managerPassword"))
      .role("Gerente general")
      .isAdmin(false)
      .associatedAccount(acc)
      .inventoryPerms(List.of(perm1ForGeneralManager, perm2ForGeneralManager))
    .build());

    // Encargado de sucursal 1
    PermissionsForInventoryEntity permForManger1 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("addProducts,editProducts,deleteProducts,editInventory")
      .inventoryReference(inv1)
    .build());
    userRepository.save(UserEntity.builder()
      .name("manager1")
      .password(encoder.encode("manager1Password"))
      .role("Encargado de sucursal norte")
      .isAdmin(false)
      .inventoryPerms(List.of(permForManger1))
    .build());
    
    // Encargado de sucursal 2
    PermissionsForInventoryEntity permForManger2 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("addProducts,editProducts,deleteProducts,editInventory")
      .inventoryReference(inv2)
    .build());
    userRepository.save(UserEntity.builder()
      .name("manager2")
      .password(encoder.encode("manager2Password"))
      .role("Encargado de sucursal centro")
      .isAdmin(false)
      .inventoryPerms(List.of(permForManger2))
    .build());

    // Farmacéuticos de sucursal 1
    PermissionsForInventoryEntity permForPharmacistsForInv1 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("editProducts")
      .inventoryReference(inv1)
    .build());
    userRepository.save(UserEntity.builder()
      .name("pharmacistsForInv1")
      .password(encoder.encode("pharmacists1Password"))
      .role("Farmacéuticos")
      .isAdmin(false)
      .inventoryPerms(List.of(permForPharmacistsForInv1))
    .build());

    // Farmacéuticos de sucursal 2
    PermissionsForInventoryEntity permForPharmacistsForInv2 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("editProducts")
      .inventoryReference(inv2)
    .build());
    userRepository.save(UserEntity.builder()
      .name("pharmacistsForInv2")
      .password(encoder.encode("pharmacists2Password"))
      .role("Farmacéuticos")
      .isAdmin(false)
      .inventoryPerms(List.of(permForPharmacistsForInv2))
    .build());

    // Cajeros de sucursal 1
    PermissionsForInventoryEntity permCashiersForInv1 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("editInventory")
      .inventoryReference(inv1)
    .build());
    userRepository.save(UserEntity.builder()
      .name("cashiersForInv1")
      .password(encoder.encode("cashiers1Password"))
      .role("Cajeros")
      .isAdmin(false)
      .inventoryPerms(List.of(permCashiersForInv1))
    .build());

    // Cajeros de sucursal 2
    PermissionsForInventoryEntity permCashiersForInv2 = permsRepository.save(PermissionsForInventoryEntity.builder()
      .permissions("editInventory")
      .inventoryReference(inv2)
    .build());
    userRepository.save(UserEntity.builder()
      .name("cashiersForInv2")
      .password(encoder.encode("cashier21Password"))
      .role("Cajeros")
      .isAdmin(false)
      .inventoryPerms(List.of(permCashiersForInv2))
    .build());
  }
}
//TODO: agregar sanitizador de strings en general