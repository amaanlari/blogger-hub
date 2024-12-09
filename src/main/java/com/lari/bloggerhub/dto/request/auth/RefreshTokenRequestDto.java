package com.lari.bloggerhub.dto.request.auth;

public class RefreshTokenRequestDto {
  private String refreshToken;

  public RefreshTokenRequestDto(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
