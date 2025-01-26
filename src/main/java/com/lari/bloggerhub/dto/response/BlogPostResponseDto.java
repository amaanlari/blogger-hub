package com.lari.bloggerhub.dto.response;

public class BlogPostResponseDto {

  private String blogPostId;
  private String title;
  private String description;
  private String bannerImageUrl;
  private String content;
  private boolean isPremium;
  private String createdBy;
  private String createdAt;
  private String updatedBy;
  private String updatedAt;

  public BlogPostResponseDto() {}

  public BlogPostResponseDto(
      String blogPostId,
      String title,
      String description,
      String bannerImageUrl,
      String content,
      boolean isPremium,
      String createdBy,
      String createdAt,
      String updatedBy,
      String updatedAt) {
    this.blogPostId = blogPostId;
    this.title = title;
    this.description = description;
    this.bannerImageUrl = bannerImageUrl;
    this.content = content;
    this.isPremium = isPremium;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
  }

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

  public String getCreatedBy() {
    return createdBy;
  }

  public boolean isPremium() {
    return isPremium;
  }

  public void setPremium(boolean premium) {
    isPremium = premium;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
