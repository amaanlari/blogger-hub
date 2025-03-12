package com.lari.bloggerhub.document;

import com.lari.bloggerhub.dto.response.BlogUserRef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.StringJoiner;

@Document("blog_post")
public class BlogPost {

  @Id String blogPostId;

  @Field("title")
  String title;

  @Field("description")
  String description;

  @Field("banner_image_url")
  String bannerImageUrl;

  String content;

  @Field("is_premium")
  boolean isPremium;

  @DocumentReference(collection = "blog_user")
  @CreatedBy
  @Field("created_by")
  BlogUserRef createdBy;

  @CreatedDate
  @Field("created_at")
  Instant createdAt = Instant.now();

  @DocumentReference(collection = "blog_user")
  @LastModifiedBy
  @Field("updated_by")
  BlogUserRef updatedBy;

  @LastModifiedDate
  @Field("updated_at")
  Instant updatedAt = Instant.now();

  public String getBlogPostId() {
    return blogPostId;
  }

  public void setBlogPostId(String blogPostId) {
    this.blogPostId = blogPostId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBannerImageUrl() {
    return bannerImageUrl;
  }

  public void setBannerImageUrl(String bannerImageUrl) {
    this.bannerImageUrl = bannerImageUrl;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public boolean isPremium() {
    return isPremium;
  }

  public void setPremium(boolean premium) {
    isPremium = premium;
  }

  public BlogUserRef getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(BlogUserRef createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public BlogUserRef getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(BlogUserRef updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BlogPost.class.getSimpleName() + "[", "]")
        .add("blogPostId='" + blogPostId + "'")
        .add("title='" + title + "'")
        .add("description='" + description + "'")
        .add("bannerImageUrl='" + bannerImageUrl + "'")
        .add("content='" + content + "'")
        .add("isPremium=" + isPremium)
        .add("createdBy=" + createdBy)
        .add("createdAt=" + createdAt)
        .add("updatedBy=" + updatedBy)
        .add("updatedAt=" + updatedAt)
        .toString();
  }
}
