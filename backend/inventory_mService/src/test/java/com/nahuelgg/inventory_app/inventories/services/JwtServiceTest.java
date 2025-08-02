package com.nahuelgg.inventory_app.inventories.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO.InventoryPermsDTO;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
  @Mock ObjectMapper objectMapper;

  @InjectMocks JwtService jwtService;

  String token;

  @BeforeEach
  void generateToken() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    Field secretKeyField = JwtService.class.getDeclaredField("SECRET_KEY");
    secretKeyField.setAccessible(true);
    secretKeyField.set(jwtService, "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=");

    Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode("YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="));

    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("accountId", "id123");
    extraClaims.put("userName", "user");
    extraClaims.put("userRole", "role");
    extraClaims.put("isAdmin", false);
    extraClaims.put("userPerms", List.of(Map.of("idOfInventoryReferenced", "inv123", "permissions", List.of("addProducts"))));

    token = Jwts.builder()
      .setClaims(extraClaims)
      .setSubject("accountUsername")
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
      .signWith(key, SignatureAlgorithm.HS256)
    .compact();
  }

  @Test
  void getClaim_subject() {
    String subject = jwtService.getClaim(token, Claims::getSubject);
    assertEquals("accountUsername", subject);
  }

  @Test
  void isTokenExpired_false() {
    assertFalse(jwtService.isTokenExpired(token));
  }

  @Test
  void mapTokenClaims() throws Exception {
    InventoryPermsDTO mockPerm = InventoryPermsDTO.builder()
      .idOfInventoryReferenced("inv123")
      .permissions(List.of(Permissions.addProducts))
    .build();

    when(objectMapper.convertValue(any(), eq(InventoryPermsDTO.class))).thenReturn(mockPerm);
    JwtClaimsDTO result = jwtService.mapTokenClaims(token);

    assertEquals("id123", result.getAccountId());
    assertEquals("user", result.getUserName());
    assertEquals("role", result.getUserRole());
    assertFalse(result.isAdmin());
    assertEquals(1, result.getUserPerms().size());
    assertEquals(mockPerm, result.getUserPerms().get(0));
  }
}
