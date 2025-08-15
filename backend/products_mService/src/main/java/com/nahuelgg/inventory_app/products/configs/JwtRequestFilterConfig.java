package com.nahuelgg.inventory_app.products.configs;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.products.dtos.JwtClaimsDTO.PermissionsForInventoryDTO;
import com.nahuelgg.inventory_app.products.services.JwtService;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.products.utilities.ContextAuthenticationPrincipal.UserSigned;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtRequestFilterConfig  extends OncePerRequestFilter {
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      String accountUsername = jwtService.getClaim(token, claims -> claims.getSubject());
      JwtClaimsDTO tokenClaims = jwtService.mapTokenClaims(token);

      String accountId = tokenClaims.getAccountId();
      String userName = tokenClaims.getUserName();
      String userRole = tokenClaims.getUserRole();
      boolean isAdmin = tokenClaims.isAdmin();
      List<PermissionsForInventoryDTO> userPerms = tokenClaims.getUserPerms();

      if (jwtService.isTokenExpired(token)) {
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
        return;
      }

      if (accountUsername != null) {
        ContextAuthenticationPrincipal newAuthData = ContextAuthenticationPrincipal.builder()
          .account(new AccountSigned(accountUsername, accountId))
          .user(new UserSigned(userName, userRole, isAdmin, userPerms))
        .build();

        PreAuthenticatedAuthenticationToken newAuth = new PreAuthenticatedAuthenticationToken(
          newAuthData, null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
      }
    } catch (Exception e) {
      System.out.println("Error al registrar el token: " + e.getMessage());
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
