package com.nahuelgg.inventory_app.users.configs;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nahuelgg.inventory_app.users.dtos.JwtClaimsDTO;
import com.nahuelgg.inventory_app.users.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtRequestFilterConfig extends OncePerRequestFilter {
  private JwtService jwtService;
  private UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
  throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    String token;
    String accountUsername;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      token = authHeader.substring(7);

      accountUsername = jwtService.getClaim(token, claim -> claim.getSubject());

      if (accountUsername == null || SecurityContextHolder.getContext().getAuthentication() != null) {
        filterChain.doFilter(request, response);
        return;
      }

      UserDetails userDetails = userDetailsService.loadUserByUsername(accountUsername);

      boolean isTokenValid = jwtService.isTokenValid(token, "");
      boolean isTokenExpired = jwtService.isTokenExpired(token);
      boolean canBeRenewed = jwtService.canTokenBeRenewed(token);

      if (!isTokenValid || (isTokenExpired && !canBeRenewed)) {
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
        return;
      }

      if (isTokenExpired) {
        JwtClaimsDTO oldTokenClaims = jwtService.mapTokenClaims(token);
        String newToken = jwtService.renewToken(token, oldTokenClaims);
        response.setHeader("Authorization", "Bearer " + newToken);
      }

      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.getAuthorities());

      authToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);

    } catch (Exception e) {
      // TODO: 
      System.out.println(e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
