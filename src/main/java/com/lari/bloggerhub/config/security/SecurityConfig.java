package com.lari.bloggerhub.config.security;

import com.lari.bloggerhub.config.security.filter.AccessTokenEntryPoint;
import com.lari.bloggerhub.config.security.filter.AccessTokenFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * This class provides the security configuration for the Blogger Hub application. It configures the
 * security filters, authentication manager, and access control policies for the application.
 *
 * <p>The security configuration includes settings for CORS, CSRF protection, and access control for
 * different endpoints in the application.
 */
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

  private final AccessTokenEntryPoint accessTokenEntryPoint;

  /**
   * Constructs a new instance of the {@link SecurityConfig} class with the specified dependencies.
   *
   * @param accessTokenEntryPoint the entry point for handling unauthorized requests
   */
  public SecurityConfig(AccessTokenEntryPoint accessTokenEntryPoint) {
    this.accessTokenEntryPoint = accessTokenEntryPoint;
  }

  /**
   * Creates a new instance of the {@link AuthenticationManager} bean using the specified
   * configuration.
   *
   * @param authenticationConfiguration the configuration for creating the authentication manager
   * @return the authentication manager bean
   * @throws Exception if an error occurs while creating the authentication manager
   */
  @Bean
  public AuthenticationManager authenticationManagerBean(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * Creates a new instance of the {@link PasswordEncoder} bean using the BCrypt algorithm.
   *
   * @return the password encoder bean
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures the security filter chain for the Blogger Hub application. The filter chain includes
   * settings for CORS, CSRF protection, and access control policies for different endpoints in the
   * application.
   *
   * @param http the HTTP security configuration
   * @param accessTokenFilter the filter for validating access tokens
   * @return the security filter chain for the application
   * @throws Exception if an error occurs while configuring the security filter chain
   */
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

  /**
   * Creates a new instance of the {@link CorsConfigurationSource} bean with the specified CORS
   * configuration.
   *
   * @return the CORS configuration source bean
   */
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
