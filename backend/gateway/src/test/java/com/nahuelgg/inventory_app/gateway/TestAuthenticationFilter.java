package com.nahuelgg.inventory_app.gateway;

import static org.mockito.Mockito.doNothing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nahuelgg.inventory_app.gateway.utilities.JwtUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class TestAuthenticationFilter {
  @Autowired
  WebTestClient webTestClient;

  @MockitoBean
  JwtUtils jwtUtils;

  @Test
  void shouldRejectIfNoToken() {
    webTestClient.get()
      .uri("/product/edit")
      .exchange()
      .expectStatus().is5xxServerError();
  }

  @Test
  void shouldPassIfTokenIsValid() {
    doNothing().when(jwtUtils).validateToken("valid-token");

    webTestClient.get()
      .uri("/product/edit")
      .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
      .exchange()
      .expectStatus().isOk();
  }

  @Test
  void shouldAllowOpenRouteWithoutToken() {
    webTestClient.get()
      .uri("/account/register")
      .exchange()
      .expectStatus().isOk();
  }
}
