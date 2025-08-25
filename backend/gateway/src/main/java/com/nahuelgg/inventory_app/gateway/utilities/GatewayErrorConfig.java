package com.nahuelgg.inventory_app.gateway.utilities;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayErrorConfig {

  @Bean
  public ErrorWebExceptionHandler globalErrorHandler(ObjectMapper objectMapper) {
    return (exchange, ex) -> {
      // Construir el body de la respuesta según la excepción
      Map<String, Object> errorBody = new HashMap<>();

      if (ex instanceof ExpiredJwtException) {
        errorBody.put("message", "Token expirado");
        errorBody.put("status", HttpStatus.UNAUTHORIZED.value());
      } else if (ex instanceof SignatureException) {
        errorBody.put("message", "Token inválido");
        errorBody.put("status", HttpStatus.FORBIDDEN.value());
      } else {
        errorBody.put("message", "Error interno del gateway. " + ex.getMessage());
        errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      }

      byte[] bytes;
      DataBuffer buffer = null;
      try {
        bytes = objectMapper.writeValueAsBytes(errorBody);
        buffer = exchange.getResponse()
          .bufferFactory()
        .wrap(bytes);
      } catch (JsonProcessingException e) {
        System.out.println(e.getMessage());
      }

      exchange.getResponse().setStatusCode(HttpStatus.valueOf((int) errorBody.get("status")));
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

      return exchange.getResponse().writeWith(Mono.just(buffer));
    };
  }
}
