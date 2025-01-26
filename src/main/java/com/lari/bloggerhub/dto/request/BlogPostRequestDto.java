package com.lari.bloggerhub.dto.request;

public class BlogPostRequestDto {

  private String title;
  private String description;
  private String bannerImageUrl;
  private String content;

  public BlogPostRequestDto() {}

  public BlogPostRequestDto(
      String title, String description, String bannerImageUrl, String content) {
    this.title = title;
    this.description = description;
    this.bannerImageUrl = bannerImageUrl;
    this.content = content;
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
}
