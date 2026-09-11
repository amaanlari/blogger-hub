package com.lari.bloggerhub.service.bloguser;

import com.cloudinary.Cloudinary;
import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.dto.request.UpdateBlogUserRequestDto;
import com.lari.bloggerhub.dto.request.auth.SignupRequestDto;
import com.lari.bloggerhub.dto.response.BlogUserResponseDto;
import com.lari.bloggerhub.dto.response.PublicUserDto;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * This class provides service methods for managing user-related operations in the Blogger Hub
 * application.
 *
 * <p>The service methods include functionality for registering new users, updating user profiles,
 * and managing user roles and permissions.
 */
@Service
public class BlogUserService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(BlogUserService.class);

  /** Upper bound on {@link #lookupUsers(List)}; comfortably above any realistic comment thread. */
  private static final int MAX_USER_LOOKUP_BATCH = 100;

  private final BlogUserRepository blogUserRepository;
  private final Cloudinary cloudinary;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  /**
   * Constructs a new instance of the {@link BlogUserService} class with the specified dependencies.
   *
   * @param blogUserRepository the repository class for managing user data
   */
  public BlogUserService(
      BlogUserRepository blogUserRepository,
      Cloudinary cloudinary,
      PasswordEncoder passwordEncoder,
      EmailService emailService) {
    this.blogUserRepository = blogUserRepository;
    this.cloudinary = cloudinary;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  /**
   * Creates a new user in the Blogger Hub application with the specified user details.
   *
   * @param userDto the user details to create
   * @throws IllegalArgumentException if the username or email is already taken
   */
  @Transactional
  public void createBlogUser(SignupRequestDto userDto) {
    // Validate if username or email is unique
    if (blogUserRepository.existsByUsername(userDto.getUsername())) {
      throw new IllegalArgumentException("Username is already taken.");
    }
    if (blogUserRepository.existsByEmail(userDto.getEmail())) {
      throw new IllegalArgumentException("Email is already registered.");
    }

    BlogUser blogUser = new BlogUser();
    BeanUtils.copyProperties(userDto, blogUser);
    blogUser.setCreatedAt(Instant.now());
    blogUser.setUpdatedAt(Instant.now());

    log.info("Creating new user with email: {}", blogUser.getEmail());
    emailService.sendVerificationEmail(blogUser.getEmail());
    blogUserRepository.save(blogUser);
  }

  /**
   * Retrieves a user by their unique identifier.
   *
   * @param userId the unique identifier of the user
   * @return a response entity containing the user details
   * @throws UsernameNotFoundException if the user is not found
   */
  public ResponseEntity<Response> getUserById(String userId) {
    BlogUser user = findById(userId);
    BlogUserResponseDto responseDto =
        new BlogUserResponseDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getProfilePicture(),
            user.getRoles());
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "User found.", responseDto));
  }

  /**
   * Retrieves a user by their username.
   *
   * @param username the username of the user
   * @return a response entity containing the user details
   * @throws UsernameNotFoundException if the user is not found
   */
  public ResponseEntity<Response> getUserByUsername(String username) {
    BlogUser user =
        blogUserRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    BlogUserResponseDto responseDto =
        new BlogUserResponseDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getProfilePicture(),
            user.getRoles());
    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "User found.", responseDto));
  }

  /**
   * Resolves a batch of user IDs to their public profiles, skipping any that no longer exist.
   *
   * <p>Skipping rather than throwing is deliberate: the caller is typically rendering a comment
   * thread, and one deleted account should not blank out the whole thread. The client is expected to
   * fall back to a placeholder byline for any ID missing from the result.
   *
   * <p>The batch is capped so that a hand-crafted query string cannot turn one public, unauthenticated
   * request into an unbounded dump of the user collection.
   *
   * @param ids the user IDs to resolve
   * @return a response entity containing the matching public profiles
   */
  public ResponseEntity<Response> lookupUsers(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return ResponseEntity.ok(
          new DataResponse(true, HttpStatus.OK.value(), "Users found.", List.of()));
    }
    if (ids.size() > MAX_USER_LOOKUP_BATCH) {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.BAD_REQUEST.value(),
                  "Too many ids requested. Maximum is " + MAX_USER_LOOKUP_BATCH + ".",
                  null));
    }

    List<PublicUserDto> users =
        blogUserRepository.findAllById(ids).stream()
            .map(
                user -> {
                  PublicUserDto dto = new PublicUserDto();
                  dto.setId(user.getId());
                  dto.setUsername(user.getUsername());
                  dto.setBio(user.getBio());
                  dto.setProfilePicture(user.getProfilePicture());
                  dto.setRoles(user.getRoles());
                  return dto;
                })
            .toList();

    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Users found.", users));
  }

  /**
   * Retrieves a list of all users in the Blogger Hub application.
   *
   * @return a response entity containing the list of users
   */
  public ResponseEntity<Response> getAllUsers() {
    // Fetch all users from the repository
    List<BlogUserResponseDto> responseDtoList =
        blogUserRepository.findAll().stream()
            .map(
                user ->
                    new BlogUserResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getBio(),
                        user.getProfilePicture(),
                        user.getRoles()))
            .toList();
    if (responseDtoList.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
          .body(new SuccessResponse(true, HttpStatus.NO_CONTENT.value(), "No users found."));
    }

    return ResponseEntity.ok(
        new DataResponse(true, HttpStatus.OK.value(), "Records found.", responseDtoList));
  }

  /**
   * Updates the profile information of a user in the Blogger Hub application.
   *
   * @param userId the unique identifier of the user
   * @param updateUserRequestDto the updated user details
   * @return a response entity indicating the success or failure of the operation
   */
  @Transactional
  public ResponseEntity<Response> updateUser(
      String userId, UpdateBlogUserRequestDto updateUserRequestDto) {
    BlogUser user = findById(userId);
    user.setUsername(updateUserRequestDto.getUsername());
    user.setEmail(updateUserRequestDto.getEmail());
    user.setPassword(passwordEncoder.encode(updateUserRequestDto.getPassword()));
    user.setBio(updateUserRequestDto.getBio());
    blogUserRepository.save(user);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "User updated successfully."));
  }

  @Transactional
  public ResponseEntity<Response> updateUser(String userId, Map<String, Object> updates) {
    BlogUser user = findById(userId);
    updates.forEach(
        (fieldName, fieldValue) -> {
          try {
            Field field = BlogUser.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, fieldValue);
          } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        });
    user.setUpdatedAt(Instant.now());
    blogUserRepository.save(user);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "User updated successfully."));
  }

  @Transactional
  public ResponseEntity<Response> updateEmail(String userId, String newEmail, String otp) {
    BlogUser currentUser = this.findById(userId);
    if (emailService.verifyEmail(currentUser, newEmail, otp)) {
      currentUser.setUpdatedAt(Instant.now());
      return ResponseEntity.ok(
          new SuccessResponse(true, HttpStatus.OK.value(), "Email updated successfully."));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(false, HttpStatus.BAD_REQUEST.value(), "Invalid OTP.", ""));
  }

  /**
   * Adds a profile picture to the user's account. The profile picture is uploaded to the Cloudinary
   * storage service and the URL is saved in the user's profile.
   *
   * @param userId the unique identifier of the user
   * @param profilePicture the profile picture to upload
   * @return a response entity indicating the success or failure of the operation
   * @throws IOException if an error occurs while uploading the profile picture
   */
  @Transactional
  public ResponseEntity<Response> uploadProfilePicture(String userId, MultipartFile profilePicture)
      throws IOException {
    String cloudinaryProfilePicDirPath = String.format("blogger_hub/%s/profile_pic", userId);
    String publicId = "profile-pic-image/" + userId;
    Map<?, ?> uploadResult;
    try {

      Map<Object, Object> params =
          Map.of(
              "public_id",
              publicId,
              "asset_folder",
              cloudinaryProfilePicDirPath,
              "overwrite",
              true);
      uploadResult = this.cloudinary.uploader().upload(profilePicture.getBytes(), params);
      String profilePictureUrl = (String) uploadResult.get("secure_url");
      blogUserRepository
          .findById(userId)
          .ifPresent(
              user -> {
                user.setProfilePicture(profilePictureUrl);
                user.setUpdatedAt(Instant.now());
                blogUserRepository.save(user);
              });
    } catch (Exception e) {
      log.error("Failed to add profile picture", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.INTERNAL_SERVER_ERROR.value(),
                  "Failed to add profile picture.",
                  e.getMessage()));
    }
    return ResponseEntity.ok(
        new DataResponse(
            true, HttpStatus.OK.value(), "Profile picture added successfully.", uploadResult));
  }

  /**
   * Removes the profile picture of a user in the Blogger Hub application. The profile picture is
   * deleted from the Cloudinary storage service and the URL is updated in the user's profile.
   *
   * <p>If the user does not have a profile picture, the method returns a bad request response. If
   * an error occurs while removing the profile picture, the method returns an internal server error
   * response. If the profile picture is successfully removed, the method returns an OK response.
   *
   * @param userId the unique identifier of the user
   * @return a response entity indicating the success or failure of the operation
   */
  @Transactional
  public ResponseEntity<Response> removeProfilePicture(String userId) {
    BlogUser currentUser = this.findById(userId);
    String profilePicUrl = currentUser.getProfilePicture();
    if (profilePicUrl.equals(Constant.DEFAULT_PROFILE_IMAGE_URL)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.BAD_REQUEST.value(),
                  "Profile picture is already removed.",
                  "User has no profile picture to remove."));
    }
    try {
      if (!profilePicUrl.startsWith("http")) {
        currentUser.setProfilePicture(Constant.DEFAULT_PROFILE_IMAGE_URL);
        currentUser.setUpdatedAt(Instant.now());
        blogUserRepository.save(currentUser);
        return ResponseEntity.ok(
            new SuccessResponse(
                true,
                HttpStatus.OK.value(),
                "Profile picture removed successfully. Profile pic is not a URL."));
      }
      String publicId = "profile-pic-image/" + currentUser.getId();
      this.cloudinary.uploader().destroy(publicId, Map.of());
      currentUser.setProfilePicture(Constant.DEFAULT_PROFILE_IMAGE_URL);
    } catch (Exception e) {
      log.error("Failed to remove profile picture", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              new ErrorResponse(
                  false,
                  HttpStatus.INTERNAL_SERVER_ERROR.value(),
                  "Failed to remove profile picture.",
                  e.getMessage()));
    }
    blogUserRepository.save(currentUser);
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Profile picture removed successfully."));
  }

  // Helper methods for fetching user details

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public BlogUser findById(String id) {
    return blogUserRepository
        .findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return blogUserRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
  }

  public ResponseEntity<Response> getAllUsersDetails() {
    return ResponseEntity.ok(
        new DataResponse(
            true,
            HttpStatus.OK.value(),
            "Records found.",
            blogUserRepository.findAll()));
  }
}
