package com.lari.bloggerhub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document
public class RefreshToken {
  @Id String id;

  @DocumentReference(lazy = true)
  private BlogUser owner;

  public RefreshToken() {}

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
