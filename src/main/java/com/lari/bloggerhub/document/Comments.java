package com.lari.bloggerhub.document;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.StringJoiner;

@Document("comments")
public class Comments {

  @Id private String id;

  @Field("post_id")
  private String postId;

  @CreatedBy
  @Field("user_id")
  private String userId;

  @Field("parent_id")
  private String parentId;

  private String content;

  @CreatedDate
  @Field("created_at")
  private Instant createdAt = Instant.now();

  public Comments() {
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

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Comments.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("postId='" + postId + "'")
        .add("userId='" + userId + "'")
        .add("comment='" + content + "'")
        .add("createdAt=" + createdAt)
        .toString();
  }
}
