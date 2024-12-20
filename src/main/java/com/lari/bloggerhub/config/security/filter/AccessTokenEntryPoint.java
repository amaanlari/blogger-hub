package com.lari.bloggerhub.config.security.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * This class represents the entry point for handling unauthorized requests to the Blogger Hub API.
 * It implements the {@link AuthenticationEntryPoint} interface to provide a custom implementation
 * for handling unauthorized requests.
 *
 * <p>The entry point sends an HTTP 401 Unauthorized response when an unauthenticated user attempts
 * to access a protected resource.
 */
@Component
public class AccessTokenEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(AccessTokenEntryPoint.class);

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    log.error("Unauthorized", authException);
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
  }
}
