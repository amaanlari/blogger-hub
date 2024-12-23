package com.lari.bloggerhub.controller.bloguser;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.dto.request.UpdateBlogUserRequestDto;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.bloguser.BlogUserService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

  private static final Logger log = LoggerFactory.getLogger(BlogUserController.class);
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

  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping("/id/{userId}")
  public ResponseEntity<Response> getUserById(
      @PathVariable String userId, Authentication authentication) {

    String currentUserId = ((BlogUser) authentication.getPrincipal()).getId();
    if (!currentUserId.equals(userId)) {
      log.error("User with id {} is not authorized to view user with id {}", currentUserId, userId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.UNAUTHORIZED.value(),
                  "You are not authorized to view this user's profile",
                  "Logged in user's id does not match the requested user's id"));
    }
    return blogUserService.getUserById(userId);
  }

  @GetMapping("/{username}")
  public ResponseEntity<Response> getUserByUsername(@PathVariable String username) {
    return blogUserService.getUserByUsername(username);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/all")
  public ResponseEntity<Response> getAllUsers() {
    return blogUserService.getAllUsers();
  }

  @PreAuthorize("hasRole('FREE_USER')")
  @PutMapping("/{userId}")
  public ResponseEntity<Response> updateUser(
      @PathVariable String userId,
      @RequestBody UpdateBlogUserRequestDto updateUserRequestDto,
      Authentication authentication) {
    String currentUserId = ((BlogUser) authentication.getPrincipal()).getId();
    if (!currentUserId.equals(userId)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.UNAUTHORIZED.value(),
                  "You are not authorized to view this user's profile",
                  "Logged in user's id does not match the requested user's id"));
    }
    return blogUserService.updateUser(userId, updateUserRequestDto);
  }

  @PreAuthorize("hasRole('FREE_USER')")
  @PostMapping("/{userId}/upload-profile-picture")
  public ResponseEntity<Response> uploadProfilePicture(
      @PathVariable String userId,
      @RequestParam MultipartFile profilePicture,
      Authentication authentication)
      throws IOException {
    String currentUserId = ((BlogUser) authentication.getPrincipal()).getId();
    if (!currentUserId.equals(userId)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.UNAUTHORIZED.value(),
                  "You are not authorized to view this user's profile",
                  "Logged in user's id does not match the requested user's id"));
    }
    return blogUserService.uploadProfilePicture(userId, profilePicture);
  }

  @PreAuthorize("hasRole('FREE_USER')")
  @PostMapping("/{userId}/remove-profile-picture")
  public ResponseEntity<Response> removeProfilePicture(
      @PathVariable String userId, Authentication authentication) {
    String currentUserId = ((BlogUser) authentication.getPrincipal()).getId();
    if (!currentUserId.equals(userId)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.UNAUTHORIZED.value(),
                  "You are not authorized to view this user's profile",
                  "Logged in user's id does not match the requested user's id"));
    }
    return blogUserService.removeProfilePicture(userId);
  }
}
