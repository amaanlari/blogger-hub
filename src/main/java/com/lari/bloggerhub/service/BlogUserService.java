package com.lari.bloggerhub.service;

import com.lari.bloggerhub.dto.request.BlogUserRequestDto;
import com.lari.bloggerhub.dto.response.BlogUserResponseDto;
import com.lari.bloggerhub.model.BlogUser;
import com.lari.bloggerhub.repository.BlogUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This class provides service methods for managing user-related operations in the Blogger Hub
 * application.
 *
 * <p>The service methods include functionality for registering new users, updating user profiles,
 * and managing user roles and permissions.
 */
@Service
public class BlogUserService implements UserDetailsService {

  private final BlogUserRepository blogUserRepository;

  //  private final PasswordEncoder passwordEncoder;

  /**
   * Constructs a new instance of the {@link BlogUserService} class with the specified dependencies.
   *
   * @param blogUserRepository the repository class for managing user data
   */
  public BlogUserService(BlogUserRepository blogUserRepository) {
    //    this.passwordEncoder = passwordEncoder;
    this.blogUserRepository = blogUserRepository;
  }

  /**
   * Creates a new user in the Blogger Hub application with the specified user details.
   *
   * @param userDto the user details to create
   */
  public void createBlogUser( BlogUserRequestDto userDto) {
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
    blogUser.setProfilePicture(userDto.getProfilePicture());

    // Save user
    blogUserRepository.save(blogUser);

    // Send verification email (optional)
    // emailService.sendVerificationEmail(blogUser.getEmail())
  }

  /**
   * Retrieves a list of all users in the Blogger Hub application.
   *
   * @return a response entity containing the list of users
   */
  public ResponseEntity<?> getBlogUsers() {
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
      return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No users found.");
    }

    return ResponseEntity.ok(responseDtoList);
  }

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
