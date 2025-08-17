package com.nahuelgg.inventory_app.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
  public HttpGraphQlClient clientToInventoryMS() {
    WebClient webClient = WebClient.builder()
      .baseUrl("http://api-inventory:8083/graphql")
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.build();

    return HttpGraphQlClient.create(webClient);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}

//TODO: hacer que el TokenDTO devuelva tambien la id de la cuenta, agregar esto en el readme