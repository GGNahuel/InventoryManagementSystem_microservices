package com.nahuelgg.inventory_app.gateway.utilities;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter> {
  private final JwtUtils jwtUtils;

  public AuthenticationFilter(JwtUtils jwtUtils) {
    super(AuthenticationFilter.class);
    this.jwtUtils = jwtUtils;
  }

  private boolean checkIfEndpointIsOpen(ServerHttpRequest request) {
    List<String> openEndpoints = List.of(
      "/account/register",
      "/authenticate/login/account",
      "/e2e"
    );

    System.out.println(request.getURI());
    System.out.println(request.getURI().getPath());
    return openEndpoints.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));
  }

  @Override
  public GatewayFilter apply(AuthenticationFilter config) {
    return ((exchange, chain) -> {
      if (checkIfEndpointIsOpen(exchange.getRequest())) {
        if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
          throw new RuntimeException("Missing authorization header");
        }

        String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          authHeader = authHeader.substring(7);
        }
        try {
          jwtUtils.validateToken(authHeader);
        } catch (Exception e) {
          System.out.println("invalid access...!");
          throw new RuntimeException("unauthorized access to application");
        }
      }
      return chain.filter(exchange);
    });
  }
}