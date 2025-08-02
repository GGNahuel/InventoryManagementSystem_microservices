package com.nahuelgg.inventory_app.inventories.services;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.inventories.dtos.UserFromUsersMSDTO.InventoryPermsDTO;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TokenGenerator {
  private final ObjectMapper objectMapper;

  @Value("${jwt_key}")
  String secretKey;

  public String generateAccountToken(String accUsername, String accId) {
    JwtClaimsDTO claims = JwtClaimsDTO.builder()
      .accountId(accId)
    .build();

    return generateToken(claims, accUsername);
  }

  public String generateAdminToken(String accUsername, String accId) {
    JwtClaimsDTO claims = JwtClaimsDTO.builder()
      .accountId(accId)
      .userName("adminUser")
      .userRole("role")
      .isAdmin(true)
    .build();

    return generateToken(claims, accUsername);
  }

  public String generateUserToken(String accUsername, String accId, List<InventoryPermsDTO> perms) {
    JwtClaimsDTO claims = JwtClaimsDTO.builder()
      .accountId(accId)
      .userName("user")
      .userRole("role")
      .isAdmin(false)
      .userPerms(perms != null ? perms : new ArrayList<>())
    .build();

    return generateToken(claims, accUsername);
  }

  public String generateToken(JwtClaimsDTO info, String username) {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    Key key = Keys.hmacShaKeyFor(keyBytes);
    
    String userPerms = null;
    try {
      userPerms = info.getUserPerms() != null ? objectMapper.writeValueAsString(info.getUserPerms()) : "[]";
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }

    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("accountId", info != null ? info.getAccountId() : null);
    extraClaims.put("userName", info != null ? info.getUserName() : null);
    extraClaims.put("userRole", info != null ? info.getUserRole() : null);
    extraClaims.put("isAdmin", info != null ? info.isAdmin() : null);
    extraClaims.put("userPerms", info != null ? userPerms : null);

    long tokenExpiration = 1000 * 60 * 5;

    return Jwts.builder()
      .setClaims(extraClaims)
      .setSubject(username)
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
      .signWith(key, SignatureAlgorithm.HS256)
    .compact();
  }
}
