package com.lari.bloggerhub.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 * Represents a refresh token entity in the <b>Blogger Hub</b> application.
 *
 * <p>This class serves as the model for storing and managing refresh token data in a MongoDB
 * database. Each refresh token is assigned a unique identifier and is associated with a user who
 * owns the token.
 */
@Document("refresh_token")
public class RefreshToken {
  @Id String id;

  @DocumentReference(lazy = true)
  private BlogUser owner;

  /** Default constructor. */
  public RefreshToken() {}

  /**
   * Initializes a new refresh token with the specified ID and owner.
   *
   * @param id the ID of the refresh token
   * @param owner the user who owns the refresh token
   */
  public RefreshToken(String id, BlogUser owner) {
    this.id = id;
    this.owner = owner;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BlogUser getOwner() {
    return owner;
  }

  public void setOwner(BlogUser owner) {
    this.owner = owner;
  }

  @Override
  public String toString() {
    return "RefreshToken{" + "id='" + id + '\'' + ", owner=" + owner + '}';
  }
}
