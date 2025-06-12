package com.nahuelgg.inventory_app.users.services;

import static com.nahuelgg.inventory_app.users.utilities.Validations.checkFieldsHasContent;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.dtos.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.users.utilities.Validations.Field;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
  private final ObjectMapper objectMapper;

  @Value("${JWT_KEY}")
  private String SECRET_KEY;
  private static final long TOKEN_EXPIRATION = 1000 * 60 * 60 * 8;
  private static final long REFRESH_WINDOW = 1000 * 60 * 60 * 24;
  
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

  public JwtClaimsDTO mapTokenClaims(String token) {
    Claims claims = getAllClaims(token);
    List<Map<String, Object>> rawPermList = claims.get("userPerms", List.class);
    List<PermissionsForInventoryDTO> convertedPerms = rawPermList.stream().map(
      rawPerm -> objectMapper.convertValue(rawPerm, PermissionsForInventoryDTO.class)
    ).toList();

    return JwtClaimsDTO.builder()
      .accountUsername(claims.getSubject())
      .userName(claims.get("userName", String.class))
      .userRole(claims.get("userRole", String.class))
      .isAdmin(claims.get("isAdmin", boolean.class))
      .userPerms(convertedPerms)
    .build();
  }

  public <T> T getClaim(String token, Function<Claims, T> claimGetter) {
    final Claims claims = getAllClaims(token);
    return claimGetter.apply(claims);
  }

  public String generateToken(JwtClaimsDTO info) {
    checkFieldsHasContent(new Field("nombre de cuenta", info.getAccountUsername()));

    String userPerms = null;
    try {
      userPerms = info.getUserPerms() != null ? objectMapper.writeValueAsString(info.getUserPerms()) : null;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }

    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("userName", info.getUserName());
    extraClaims.put("userRole", info.getUserRole());
    extraClaims.put("isAdmin", info.isAdmin());
    extraClaims.put("userPerms", userPerms);

    return Jwts.builder()
      .setClaims(extraClaims)
      .setSubject(info.getAccountUsername())
      .setIssuedAt(new Date(System.currentTimeMillis()))
      .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION))
      .signWith(getSignInKey(), SignatureAlgorithm.HS256)
    .compact();
  }

  public boolean isTokenValid(String token, String accountUsername) {
    final String username = getClaim(token, claims -> claims.getSubject());
    return (username.equals(accountUsername));
  }

  public boolean isTokenExpired(String token) {
    return getClaim(token, claims -> claims.getExpiration()).before(new Date());
  }

  public boolean canTokenBeRenewed(String token) {
    try {
      Claims claims = getAllClaims(token);
      Date expiration = claims.getExpiration();
      long currentTime = System.currentTimeMillis();

      return expiration.before(new Date(currentTime)) && expiration.getTime() + REFRESH_WINDOW > currentTime;
    } catch (Exception e) {
      return false;
    }
  }

  public String renewToken(String token, JwtClaimsDTO info) {
    if (!canTokenBeRenewed(token)) 
      throw new IllegalArgumentException("The JWT couldn't be renewed");

    return generateToken(info);
  }
}
