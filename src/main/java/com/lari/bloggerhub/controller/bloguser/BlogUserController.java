package com.lari.bloggerhub.controller.bloguser;

import com.lari.bloggerhub.service.bloguser.BlogUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * This class defines the REST API endpoints for managing user-related operations in the Blogger Hub
 * application.
 *
 * <p>The endpoints allow users to register, log in, and update their profile information. They also
 * provide functionality for administrators to manage user accounts, such as promoting users to
 * premium status or revoking their access.
 */
@RestController
@RequestMapping("/api/users")
public class BlogUserController {

  private final BlogUserService blogUserService;

  /**
   * Constructs a new instance of the {@link BlogUserController} class with the specified {@link
   * BlogUserService} dependency.
   *
   * @param blogUserService the service class for managing user-related operations
   */
  public BlogUserController(BlogUserService blogUserService) {
    this.blogUserService = blogUserService;
  }

  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Service is up and running");
  }
}
