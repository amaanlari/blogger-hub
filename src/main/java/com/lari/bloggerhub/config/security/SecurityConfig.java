package com.lari.bloggerhub.config.security;

import com.lari.bloggerhub.config.filter.AccessTokenEntryPoint;
import com.lari.bloggerhub.config.filter.AccessTokenFilter;
import com.lari.bloggerhub.service.BlogUserService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] AUTH_WHITELIST = {
    "/swagger-resources/**",
    "/swagger-ui.html",
    "/v2/api-docs",
    "/webjars/**",
    "/swagger-ui/**",
    "/api/auth/**"
  };

  private BlogUserService blogUserService;
  private AccessTokenEntryPoint accessTokenEntryPoint;

  public SecurityConfig(
      BlogUserService blogUserService, AccessTokenEntryPoint accessTokenEntryPoint) {
    this.blogUserService = blogUserService;
    this.accessTokenEntryPoint = accessTokenEntryPoint;
  }

  @Bean
  public AuthenticationManager authenticationManagerBean(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AccessTokenFilter accessTokenFilter) throws Exception {
    http.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
        .csrf(CsrfConfigurer::disable)
        .exceptionHandling(
            exceptionHandlingConfigurer ->
                exceptionHandlingConfigurer.authenticationEntryPoint(accessTokenEntryPoint))
        .sessionManagement(
            sessionManagementConfigurer ->
                sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers(AUTH_WHITELIST)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
