package com.lari.bloggerhub.dto.request.auth;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class RefreshTokenRequestDto {
  private String refreshToken;

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
