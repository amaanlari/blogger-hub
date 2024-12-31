package com.lari.bloggerhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * This class represents the data transfer object (DTO) for updating a user in the Blogger Hub
 * application.
 *
 * <p>The class contains fields for the user's username, email address, password, and bio.
 */
public class UpdateBlogUserRequestDto {

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
}
