package com.nahuelgg.inventory_app.users.configs;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.services.JwtService;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.AccountSigned;
import com.nahuelgg.inventory_app.users.utilities.ContextAuthenticationPrincipal.UserSigned;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtRequestFilterConfig extends OncePerRequestFilter {
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
  throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      String usernameInToken = jwtService.getClaim(token, claim -> claim.getSubject());

      if (usernameInToken == null) {
        filterChain.doFilter(request, response);
        return;
      }

      JwtClaimsDTO tokenClaims = jwtService.mapTokenClaims(token);
      boolean isTokenValid = jwtService.isTokenValid(token, usernameInToken);
      boolean isTokenExpired = jwtService.isTokenExpired(token);
      boolean canBeRenewed = jwtService.canTokenBeRenewed(token);

      if (!isTokenValid || (isTokenExpired && !canBeRenewed)) {
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
        return;
      }

      if (isTokenExpired) {
        String newToken = jwtService.renewToken(token, tokenClaims);
        response.setHeader("Authorization", "Bearer " + newToken);
      }

      Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
      if (existingAuth != null && existingAuth.getName().equals(usernameInToken)) {
        filterChain.doFilter(request, response);
        return;
      }

      UserSigned userToNewAuthData = tokenClaims.getUserName() == null ? null : 
        new UserSigned(
          tokenClaims.getUserName(), tokenClaims.getUserRole(), tokenClaims.isAdmin(), 
          tokenClaims.getUserPerms()
        );

      ContextAuthenticationPrincipal newAuthData = ContextAuthenticationPrincipal.builder()
        .account(new AccountSigned(usernameInToken, ""))
        .user(userToNewAuthData)
      .build();

      PreAuthenticatedAuthenticationToken newAuth = new PreAuthenticatedAuthenticationToken(
        newAuthData,
        null
      );
      newAuth.setAuthenticated(true);
      newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(newAuth);
    } catch (Exception e) {
      // TODO: 
      System.out.println(e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
