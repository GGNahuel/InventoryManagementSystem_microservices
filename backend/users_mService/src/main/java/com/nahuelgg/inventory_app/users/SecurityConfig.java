package com.nahuelgg.inventory_app.users;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nahuelgg.inventory_app.users.utilities.Generators;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain setFilterChainProps(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
      .authorizeHttpRequests(request -> request
        .anyRequest().permitAll()
      )
      .rememberMe(remember -> remember
        .key(Generators.generateKey(16))
        .tokenValiditySeconds(60 * 60 * 2)
      )
      .csrf(csrf -> csrf.disable());

    return httpSecurity.build();
  }

  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

