package com.lari.bloggerhub.dto.response;

import com.lari.bloggerhub.enums.Role;
import java.util.List;

/**
 * The safe-to-show-anyone projection of a user: enough to render a byline or an avatar, nothing
 * more.
 *
 * <p>Distinct from {@link BlogUserResponseDto}, which includes {@code email} and is returned by the
 * publicly whitelisted {@code GET /api/users/{username}} — meaning any anonymous caller can read
 * any user's email address today. This DTO exists so that the batch lookup below, which is designed
 * to be called for every author in a comment thread, does not widen that exposure any further.
 */
public class PublicUserDto {

  private String id;
  private String username;
  private String bio;
  private String profilePicture;
  private List<Role> roles;

  public PublicUserDto() {}

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
