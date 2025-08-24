package com.nahuelgg.inventory_app.gateway.utilities;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.ExpiredJwtException;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter> {
  private final JwtUtils jwtUtils;
  private final Environment env;

  public AuthenticationFilter(JwtUtils jwtUtils, Environment env) {
    super(AuthenticationFilter.class);
    this.jwtUtils = jwtUtils;
    this.env = env;
  }

  private boolean checkIfEndpointIsNotOpen(ServerHttpRequest request) {
    List<String> openEndpoints = List.of(
      "/account/register",
      "/authenticate/login/account",
      "/authenticate/logout/account",
      "/e2e"
    );

    System.out.println(request.getURI());
    System.out.println(request.getURI().getPath());
    return openEndpoints.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));
  }

  @Override
  public GatewayFilter apply(AuthenticationFilter config) {
    return ((exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();
      String path = request.getURI().getPath();

      String[] profiles = env.getActiveProfiles();

      if (path.contains("/graphql") && profiles[0].equals("e2e")) {
        Mono<String> bodyOfGraphQlRequest =request.getBody().map(dataBuffer -> {
          byte[] bytes = new byte[dataBuffer.readableByteCount()];
          dataBuffer.read(bytes);
          DataBufferUtils.release(dataBuffer);
          return new String(bytes, StandardCharsets.UTF_8);
        }).reduce(String::concat);

        return bodyOfGraphQlRequest.flatMap(body -> {
          boolean isE2E = body.contains("query e2e-");
          if (isE2E) {
            return chain.filter(exchange);
          }
          checkIfEndpointIsNotOpen(request);
          return chain.filter(exchange);
        });
      }

      if (checkIfEndpointIsNotOpen(request)) {
        continueWithAuthCheck(exchange, chain);
      }
      return chain.filter(exchange);
    });
  }

  private void continueWithAuthCheck(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
      throw new RuntimeException("El header de autorización no se encuentra en la solicitud");
    }

    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      authHeader = authHeader.substring(7);
    }

    try {
      jwtUtils.validateToken(authHeader);
    } catch (ExpiredJwtException e) {
      throw new RuntimeException("Token expirado");
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new RuntimeException("Error al verificar el token en la gateway: " + e.getMessage());
    }
  }
}