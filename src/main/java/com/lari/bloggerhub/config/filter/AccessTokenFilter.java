package com.lari.bloggerhub.config.filter;

import com.lari.bloggerhub.model.BlogUser;
import com.lari.bloggerhub.service.BlogUserService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AccessTokenFilter.class);

  private final JwtHelper jwtHelper;
  private final BlogUserService userService;

  public AccessTokenFilter(JwtHelper jwtHelper, BlogUserService userService) {
    this.jwtHelper = jwtHelper;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
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
