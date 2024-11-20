package com.lari.bloggerhub.dto.response;

import com.lari.bloggerhub.model.Role;

import java.util.List;

/**
 * This class represents the data transfer object (DTO) for returning user details in the Blogger
 * Hub application.
 *
 * <p>The class contains fields for the user's ID, username, email address, biography, profile
 * picture, and roles.
 */
public class BlogUserResponseDto {
  private String id;
  private String username;
  private String email;
  private String bio;
  private String profilePicture;
  private List<Role> roles;

  /** Default constructor. */
  public BlogUserResponseDto() {}

  /**
   * Initializes a new user response DTO with the specified details.
   *
   * @param id the ID of the user
   * @param username the username of the user
   * @param email the email address of the user
   * @param bio the biography of the user
   * @param profilePicture the URL of the user's profile picture
   * @param roles the roles assigned to the user
   */
  public BlogUserResponseDto(
      String id,
      String username,
      String email,
      String bio,
      String profilePicture,
      List<Role> roles) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.bio = bio;
    this.profilePicture = profilePicture;
    this.roles = roles;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }
}
