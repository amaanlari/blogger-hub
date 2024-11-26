package com.lari.bloggerhub.config.security.filter;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.service.BlogUserService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This class represents a filter that intercepts incoming requests to the Blogger Hub API and
 * extracts the access token from the Authorization header. The filter validates the access token
 * and authenticates the user if the token is valid.
 *
 * <p>If the access token is invalid or the user is not found, the filter logs a warning message.
 *
 * <p>The filter extends the {@link OncePerRequestFilter} class provided by Spring Security to
 * ensure that it is only executed once per request.
 *
 * <p>The filter is registered in the Spring Security configuration to intercept all incoming
 * requests to the API and authenticate users based on the access token provided in the
 * Authorization
 */
@Component
public class AccessTokenFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AccessTokenFilter.class);

  private final JwtHelper jwtHelper;
  private final BlogUserService userService;

  /**
   * Constructs a new instance of the {@link AccessTokenFilter} class with the specified
   * dependencies.
   *
   * @param jwtHelper the helper class for parsing and validating JWT tokens
   * @param userService the service class for managing user-related operations
   */
  public AccessTokenFilter(JwtHelper jwtHelper, BlogUserService userService) {
    this.jwtHelper = jwtHelper;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Optional<String> accessToken = parseAccessToken(request);

      if (accessToken.isEmpty()) {
        log.warn("No access token found in Authorization header");
      } else if (!jwtHelper.validateAccessToken(accessToken.get())) {
        log.warn("Invalid access token: {}", accessToken.get());
      } else {
        String userId = jwtHelper.getUserIdFromAccessToken(accessToken.get());
        BlogUser user = userService.findById(userId);

        if (user == null) {
          log.warn("User not found for ID: {}", userId);
        } else {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
          log.debug("User authenticated successfully: {}", userId);
        }
      }
    } catch (Exception e) {
      log.error("Cannot authenticate user", e);
    }

    filterChain.doFilter(request, response);
  }

  private Optional<String> parseAccessToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    log.warn("authHeader: {}", authHeader);
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      return Optional.of(authHeader.replace("Bearer ", ""));
    }
    return Optional.empty();
  }
}
