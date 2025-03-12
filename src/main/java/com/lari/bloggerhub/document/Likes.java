package com.lari.bloggerhub.document;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.StringJoiner;

@Document("likes")
@CompoundIndex(def = "{'post_id': 1, 'user_id': 1}", unique = true)
public class Likes {

  @Id
  private String id;

  @Field("post_id")
//  @DocumentReference(collection = "blog_post", lazy = true)
  private String postId;

  @Field("user_id")
  @CreatedBy
//  @DocumentReference(collection = "blog_user", lazy = true)
  private String userId;

  @Field("created_at")
  @CreatedDate
  private Instant createdAt = Instant.now();

  public Likes() {
    // Default constructor
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Likes.class.getSimpleName() + "{", "}")
        .add("\nid='" + id + "'")
        .add("\npostId='" + postId + "'")
        .add("\nuserId='" + userId + "'")
        .add("\ncreatedAt=" + createdAt)
        .toString();
  }
}
