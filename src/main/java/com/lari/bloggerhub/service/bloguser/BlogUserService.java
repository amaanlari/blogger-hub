package com.lari.bloggerhub.service.bloguser;

import com.cloudinary.Cloudinary;
import com.lari.bloggerhub.constant.Constant;
import com.lari.bloggerhub.dto.request.UpdateBlogUserRequestDto;
import com.lari.bloggerhub.dto.request.auth.SignupRequestDto;
import com.lari.bloggerhub.dto.response.BlogUserResponseDto;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
  private final BlogUserRepository blogUserRepository;
  private final Cloudinary cloudinary;
  private final PasswordEncoder passwordEncoder;

  /**
   * Constructs a new instance of the {@link BlogUserService} class with the specified dependencies.
   *
   * @param blogUserRepository the repository class for managing user data
   */
  public BlogUserService(BlogUserRepository blogUserRepository, Cloudinary cloudinary, PasswordEncoder passwordEncoder) {
    this.blogUserRepository = blogUserRepository;
    this.cloudinary = cloudinary;
    this.passwordEncoder = passwordEncoder;
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

    // Convert DTO to Entity
    BlogUser blogUser = new BlogUser();
    blogUser.setUsername(userDto.getUsername());
    blogUser.setEmail(userDto.getEmail());
    blogUser.setPassword(userDto.getPassword());
    blogUser.setBio(userDto.getBio());
    blogUser.setProfilePicture(Constant.DEFAULT_PROFILE_IMAGE_URL);

    // Save user
    blogUserRepository.save(blogUser);

    // Send verification email (optional)
    // emailService.sendVerificationEmail(blogUser.getEmail())
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

  // Update email with OTP verification

  /**
   * Adds a profile picture to the user's account. The profile picture is uploaded to the Cloudinary
   * storage service and the URL is saved in the user's profile.
   *
   * @param userId the unique identifier of the user
   * @param profilePicture the profile picture to upload
   * @return a response entity indicating the success or failure of the operation
   * @throws IOException if an error occurs while uploading the profile picture
   */
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
    return ResponseEntity.ok(
        new SuccessResponse(true, HttpStatus.OK.value(), "Profile picture removed successfully."));
  }

  // Helper methods for fetching user details

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
}
