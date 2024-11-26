package com.lari.bloggerhub.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents a user entity in the <b>Blogger Hub</b> application.
 *
 * <p>This class serves as the user model for storing and managing user data in a MongoDB database.
 * It implements the {@link UserDetails} interface to integrate with Spring Security for
 * authentication and authorization purposes.
 *
 * <p>Each user is assigned a unique identifier and can have roles such as FREE_USER or
 * PREMIUM_USER, which define their access level. The class also includes metadata such as
 * timestamps for creation and updates.
 */
@Document("blog_user")
public class BlogUser implements UserDetails {

  @Id private String id;

  @Indexed(unique = true)
  private String username;

  @Indexed(unique = true)
  private String email;

  private String password;
  private String bio;
  private String profilePicture;
  private boolean isVerified;
  private List<Role> roles;

  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;

  /** Default constructor initializing the user with a FREE_USER role. */
  public BlogUser() {
    roles = new ArrayList<>();
    roles.add(Role.FREE_USER);
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

  public boolean isVerified() {
    return isVerified;
  }

  public void setVerified(boolean verified) {
    isVerified = verified;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Retrieves the authorities granted to the user based on their roles.
   *
   * <p>This method converts the user's roles into {@link SimpleGrantedAuthority} objects, which
   * Spring Security uses to enforce role-based access control.
   *
   * @return a collection of {@link GrantedAuthority} objects.
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList();
  }

  @Override
  public String toString() {
    return "BlogUser{"
        + "id='"
        + id
        + '\''
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", bio='"
        + bio
        + '\''
        + ", profilePicture='"
        + profilePicture
        + '\''
        + ", isVerified="
        + isVerified
        + ", createdAt="
        + createdAt
        + ", updatedAt="
        + updatedAt
        + '}';
  }
}
