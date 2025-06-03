package com.nahuelgg.inventory_app.users;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nahuelgg.inventory_app.users.utilities.Constants;
import com.nahuelgg.inventory_app.users.utilities.Generators;

import jakarta.servlet.http.HttpServletResponse;

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
      .formLogin(form -> form
        .loginProcessingUrl(Constants.endpointPrefix + "/logincheck")
        .successHandler((request, response, auth) -> {
          response.setStatus(HttpServletResponse.SC_OK);
        })
        .failureHandler((request, response, authEx) -> {
          if (authEx instanceof BadCredentialsException) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          } else {
            System.out.println(authEx.getClass());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          }
        })
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl(Constants.endpointPrefix + "/logout")
        .logoutSuccessHandler((request, response, auth) -> {
          response.setStatus(HttpServletResponse.SC_OK);
        })
        .permitAll()
      )
      .rememberMe(remember -> remember
        .key(Generators.generateKey(16))
        .tokenValiditySeconds(60 * 60 * 8)
      )
      .csrf(csrf -> csrf.disable());

    return httpSecurity.build();
  }

  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

