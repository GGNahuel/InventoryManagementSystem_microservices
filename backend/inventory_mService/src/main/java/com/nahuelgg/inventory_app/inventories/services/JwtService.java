package com.nahuelgg.inventory_app.inventories.services;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.inventories.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.inventories.dtos.responsesFromOtherServices.UserFromUsersMSDTO.InventoryPermsDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
  private final ObjectMapper objectMapper;

  @Value("${jwt_key}")
  private String SECRET_KEY;
  
  private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private Claims getAllClaims(String token) {
    try {
      return Jwts.parserBuilder()
        .setSigningKey(getSignInKey())
      .build()
        .parseClaimsJws(token)
      .getBody();
    } catch (ExpiredJwtException e) {
      return e.getClaims();
    } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
      throw new RuntimeException("Invalid JWT token or mal formed", e);
    }
  }

  public JwtClaimsDTO mapTokenClaims(String token) throws JsonMappingException, JsonProcessingException {
    Claims claims = getAllClaims(token);
    System.out.println(claims.get("userPerms").getClass());
    List<InventoryPermsDTO> convertedPerms = objectMapper.readValue(
      claims.get("userPerms", String.class), 
      new TypeReference<List<InventoryPermsDTO>>() {}
    );

    return JwtClaimsDTO.builder()
      .accountId(claims.get("accountId", String.class))
      .userName(claims.get("userName", String.class))
      .userRole(claims.get("userRole", String.class))
      .isAdmin(claims.get("isAdmin", Boolean.class))
      .userPerms(convertedPerms)
    .build();
  }

  public <T> T getClaim(String token, Function<Claims, T> claimGetter) {
    final Claims claims = getAllClaims(token);
    return claimGetter.apply(claims);
  }
  
  public boolean isTokenExpired(String token) {
    return getClaim(token, claims -> claims.getExpiration()).before(new Date());
  }
}
