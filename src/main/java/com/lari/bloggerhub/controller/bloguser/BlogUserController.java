package com.lari.bloggerhub.controller.bloguser;

import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.dto.request.UpdateBlogUserRequestDto;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.bloguser.BlogUserService;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

  /**
   * This method is used to check the health of the service.
   *
   * @return a {@link ResponseEntity} containing the response data
   */
  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Service is up and running");
  }

  /**
   * This method is used to get a user by their ID.
   *
   * @param userId the ID of the user to retrieve
   * @param authentication the authentication object for the current user
   * @return a {@link ResponseEntity} containing the response data
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @GetMapping("/id/{userId}")
  public ResponseEntity<Response> getUserById(
      @PathVariable String userId, Authentication authentication) {
    var isAuthorized = checkUserId(userId, authentication);
    return isAuthorized != null ? isAuthorized : blogUserService.getUserById(userId);
  }

  /**
   * This method is used to get a user by their username.
   *
   * @param username the username of the user to retrieve
   * @return a {@link ResponseEntity} containing the response data
   */
  @GetMapping("/{username}")
  public ResponseEntity<Response> getUserByUsername(@PathVariable String username) {
    return blogUserService.getUserByUsername(username);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/all")
  public ResponseEntity<Response> getAllUsers() {
    return blogUserService.getAllUsers();
  }

  /**
   * This method is used to update a user's profile information.
   *
   * @param userId the ID of the user to update
   * @param updateUserRequestDto the request data containing the updated user information
   * @param authentication the authentication object for the current user
   * @return a {@link ResponseEntity} containing the response data
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PutMapping("/{userId}")
  public ResponseEntity<Response> updateUser(
      @PathVariable String userId,
      @RequestBody UpdateBlogUserRequestDto updateUserRequestDto,
      Authentication authentication) {
    var isAuthorized = checkUserId(userId, authentication);
    return isAuthorized != null
        ? isAuthorized
        : blogUserService.updateUser(userId, updateUserRequestDto);
  }

  @PreAuthorize("hasRole('FREE_USER')")
  @PatchMapping("/{userId}")
  public ResponseEntity<Response> updateUser(
      @PathVariable String userId,
      @RequestBody Map<String, Object> request,
      Authentication authentication) {
    var isAuthorized = checkUserId(userId, authentication);
    return isAuthorized != null ? isAuthorized : blogUserService.updateUser(userId, request);
  }

  /**
   * This method is used to upload a profile picture for a user.
   *
   * @param userId the ID of the user to update
   * @param profilePicture the profile picture file to upload
   * @param authentication the authentication object for the current user
   * @return a {@link ResponseEntity} containing the response data
   * @throws IOException if an error occurs while processing the file
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PostMapping("/{userId}/upload-profile-picture")
  public ResponseEntity<Response> uploadProfilePicture(
      @PathVariable String userId,
      @RequestParam MultipartFile profilePicture,
      Authentication authentication)
      throws IOException {
    var isAuthorized = checkUserId(userId, authentication);
    return isAuthorized != null
        ? isAuthorized
        : blogUserService.uploadProfilePicture(userId, profilePicture);
  }

  /**
   * This method is used to remove a profile picture for a user.
   *
   * @param userId the ID of the user to update
   * @param authentication the authentication object for the current user
   * @return a {@link ResponseEntity} containing the response data
   */
  @PreAuthorize("hasRole('FREE_USER')")
  @PostMapping("/{userId}/remove-profile-picture")
  public ResponseEntity<Response> removeProfilePicture(
      @PathVariable String userId, Authentication authentication) {
    var isAuthorized = checkUserId(userId, authentication);
    return isAuthorized != null ? isAuthorized : blogUserService.removeProfilePicture(userId);
  }

  private static ResponseEntity<Response> checkUserId(
      String userId, Authentication authentication) {
    String currentUserId = ((BlogUser) authentication.getPrincipal()).getId();
    if (!currentUserId.equals(userId)) {
      log.error(
          "User with id {} is not authorized to access user with id {}", currentUserId, userId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.UNAUTHORIZED.value(),
                  Constant.NOT_AUTHORIZED_TO_ACCESS_PROFILE,
                  Constant.LOGGED_IN_USER_ID_DOES_NOT_MATCH_REQUESTED_USER_ID));
    }
    return null;
  }
}
