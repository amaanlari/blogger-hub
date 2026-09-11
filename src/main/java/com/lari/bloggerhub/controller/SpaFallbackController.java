package com.lari.bloggerhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets React Router handle client-side navigation. Spring Boot serves the compiled SPA out of
 * classpath:/static/ (see the frontend Maven build), which already covers real files like
 * {@code /}, {@code /index.html}, and {@code /assets/*.js} on its own. What it does NOT cover is
 * a direct browser navigation/refresh on a client-only route such as {@code /posts/123} or
 * {@code /u/jane} — those have no matching static file, so without this controller they would
 * 404 instead of loading the app.
 *
 * <p>Every mapping here lives in the same {@code RequestMappingHandlerMapping} as every real
 * {@code @RestController}, and Spring always prefers the most specific matching pattern among
 * candidates in that mapping — so none of this ever intercepts a real, existing REST endpoint.
 * The explicit {@code /api/**} mapping exists only to preserve a proper 404 (instead of
 * silently returning the SPA shell) for a genuinely mistyped or removed API path.
 *
 * <p>Spring's {@code PathPattern} parser (the default since Spring Boot 3) forbids anything
 * after a {@code **} segment, so a single "any depth, then one more segment" pattern isn't
 * expressible. Single-segment client routes (e.g. {@code /login}, {@code /notifications}) are
 * handled generically below. Multi-segment routes are matched by explicit prefix — this list
 * must stay in sync with the nested route prefixes defined in frontend/src/App.tsx.
 */
@Controller
public class SpaFallbackController {

  @GetMapping("/api/**")
  public ResponseEntity<Void> apiNotFound() {
    return ResponseEntity.notFound().build();
  }

  /** Any single, extension-less path segment — covers every current and future top-level route. */
  @GetMapping("/{path:[^\\.]*}")
  public String forwardTopLevelRoute() {
    return "forward:/index.html";
  }

  /**
   * Nested route prefixes from frontend/src/App.tsx: /u/:username/**, /posts/:id/**, /admin/**,
   * and /blog/:id (an alias that redirects to /posts/:id — see frontend/src/app/aliases.tsx).
   */
  @GetMapping({"/u/**", "/posts/**", "/admin/**", "/blog/**"})
  public String forwardNestedRoute() {
    return "forward:/index.html";
  }
}
