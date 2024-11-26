package com.lari.bloggerhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * This class represents the data transfer object (DTO) for creating a new user in the Blogger Hub
 * application.
 *
 * <p>The class contains fields for the user's username, email address, password, bio, and profile
 * picture.
 */
public class BlogUserRequestDto {

  @NotBlank
  @Size(min = 3, max = 20)
  private String username;

  @NotBlank
  @Size(max = 60)
  @Email
  private String email;

  @NotBlank
  @Size(min = 6, max = 40)
  private String password;

  @Size(max = 300)
  private String bio;

  @NotBlank private String profilePicture;

  /** Default constructor. */
  public BlogUserRequestDto() {}

  /**
   * Initializes a new user request DTO with the specified details.
   *
   * @param username the username of the user
   * @param email the email address of the user
   * @param password the password of the user
   * @param bio the biography of the user
   * @param profilePicture the URL of the user's profile picture
   */
  public BlogUserRequestDto(
      String username, String email, String password, String bio, String profilePicture) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.bio = bio;
    this.profilePicture = profilePicture;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getProfilePicture() {
    return profilePicture;
  }

  public void setProfilePicture(String profilePicture) {
    this.profilePicture = profilePicture;
  }
}
