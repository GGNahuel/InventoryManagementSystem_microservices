package com.nahuelgg.inventory_app.users.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.enums.Permissions;

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
  Key key;

  @BeforeEach
  void generateTokenManually() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    Field secretKeyField = JwtService.class.getDeclaredField("SECRET_KEY");
    secretKeyField.setAccessible(true);
    secretKeyField.set(jwtService, "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=");

    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode("YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="));

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
  void isTokenExpired() {
    assertFalse(jwtService.isTokenExpired(token));
  }

  @Test
  void isTokenValid() {
    assertTrue(jwtService.isTokenValid(token, "accountUsername"));
    assertFalse(jwtService.isTokenValid(token, "anotherUsername"));
  }

  @Test
  void mapTokenClaims() throws Exception {
    PermissionsForInventoryDTO mockPerm = PermissionsForInventoryDTO.builder()
      .idOfInventoryReferenced("inv123")
      .permissions(List.of(Permissions.addProducts))
    .build();

    when(objectMapper.convertValue(any(), eq(PermissionsForInventoryDTO.class))).thenReturn(mockPerm);
    JwtClaimsDTO result = jwtService.mapTokenClaims(token);

    assertEquals("id123", result.getAccountId());
    assertEquals("user", result.getUserName());
    assertEquals("role", result.getUserRole());
    assertFalse(result.isAdmin());
    assertEquals(1, result.getUserPerms().size());
    assertEquals(mockPerm, result.getUserPerms().get(0));
  }

  @Test
  void generateToken() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any())).thenReturn("[]");
    JwtClaimsDTO claims = JwtClaimsDTO.builder()
      .accountId("account-123")
      .userName("john")
      .userRole("role")
      .isAdmin(false)
      .userPerms(List.of())
    .build();

    String username = "userAccount";

    String actual = jwtService.generateToken(claims, username);
    assertNotNull(actual);

    Claims actualClaims = Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(actual)
    .getBody();

    assertEquals(actualClaims.getSubject(), "userAccount");
    assertEquals(actualClaims.get("userName", String.class), "john");
    assertEquals(actualClaims.get("userRole", String.class), "role");
    assertFalse(actualClaims.get("isAdmin", Boolean.class));
    assertNotNull(actualClaims.get("userPerms", String.class));
  }
}
