package com.nahuelgg.inventory_app.products.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtRequestFilterConfig jwtRequestFilterConfig;

  @Bean
  SecurityFilterChain setFilterChainProps(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
      .authorizeHttpRequests(request -> request
        .anyRequest().authenticated()
      )
      .addFilterBefore(jwtRequestFilterConfig, UsernamePasswordAuthenticationFilter.class)
      .csrf(csrf -> csrf.disable());

    return httpSecurity.build();
  }
}
