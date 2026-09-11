package com.lari.bloggerhub.dto.response;

import java.time.Instant;

/**
 * A post projection for feed/list views. Deliberately omits {@code content}.
 *
 * <p>Two reasons it is not {@link BlogPostResponseDto}. First, a feed never renders full post
 * bodies, so shipping them makes every page load carry kilobytes per card for nothing. Second, and
 * more importantly, {@code BlogPostResponseDto} carries {@code content} even for premium posts —
 * {@code GET /api/blogposts/{username}} applies no premium filtering at all, so the paywall on
 * {@code GET /api/blogposts/post/{id}} is bypassable through any list endpoint that uses it.
 * Because this DTO has no content field, the list endpoint built on it is safe to serve to
 * anonymous visitors.
 */
public class BlogPostSummaryDto {

  private String blogPostId;
  private String title;
  private String description;
  private String bannerImageUrl;
  private boolean isPremium;
  private String createdBy;
  private Instant createdAt;
  private Instant updatedAt;

  public BlogPostSummaryDto() {}

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

  public boolean isPremium() {
    return isPremium;
  }

  public void setPremium(boolean premium) {
    isPremium = premium;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
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
}
