package com.lari.bloggerhub.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Request DTO for blocking a user. */
public class BlockUserRequestDto {

  @NotBlank(message = "User ID to block is required")
  private String userId;

  private String reason; // Optional reason for blocking

  public BlockUserRequestDto() {
    // Default constructor
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
