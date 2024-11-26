package com.lari.bloggerhub.dto.response;

import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * This class represents the response for the access and refresh tokens in the Blogger Hub
 * application.
 *
 * <p>It contains the user ID, access token, and refresh token.
 */
@JsonRootName("token")
public class TokenResponseDto {
  private String userId;
  private String accessToken;
  private String refreshToken;

  /**
   * Initializes a new token response with the specified data.
   *
   * @param userId - the user ID
   * @param accessToken - the access token
   * @param refreshToken - the refresh token
   */
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
