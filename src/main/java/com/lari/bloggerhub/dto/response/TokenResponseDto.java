package com.lari.bloggerhub.dto.response;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("token")
public class TokenResponseDto {
  private String userId;
  private String accessToken;
  private String refreshToken;

  public TokenResponseDto(String userId, String accessToken, String refreshToken) {
    this.userId = userId;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
