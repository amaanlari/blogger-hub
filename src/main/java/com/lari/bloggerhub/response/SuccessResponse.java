package com.lari.bloggerhub.response;

/** This class represents the success response schema for the Blogger Hub application. */
public class SuccessResponse implements Response {

  private boolean success;
  private int statusCode;
  private String message;

  /**
   * Initializes a new success response with the specified data.
   *
   * @param success the status of the response
   * @param statusCode the status code of the response
   * @param message the message associated with the response
   */
  public SuccessResponse(boolean success, int statusCode, String message) {
    this.success = success;
    this.statusCode = statusCode;
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
