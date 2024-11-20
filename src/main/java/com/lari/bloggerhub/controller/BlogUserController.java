package com.lari.bloggerhub.controller;

import com.lari.bloggerhub.dto.request.BlogUserRequestDto;
import com.lari.bloggerhub.service.BlogUserService;
import org.springframework.http.HttpStatus;
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

  /**
   * Registers a new user in the Blogger Hub application with the specified user details.
   *
   * @param userDto the user details to register
   * @return a response entity indicating the outcome of the registration process
   */
  @PostMapping("/sign-up")
  public ResponseEntity<String> registerUser(@RequestBody BlogUserRequestDto userDto) {
    blogUserService.createBlogUser(userDto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body("User registered successfully. Please verify your email.");
  }

  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Service is up and running");
  }
}
