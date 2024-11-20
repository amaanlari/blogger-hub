package com.lari.bloggerhub.response;

public class ErrorResponse implements Response {

  private boolean success;
  private int statusCode;
  private String message;
  private String error;

  /**
   * Initializes a new error response with the specified data.
   *
   * @param success the status of the response
   * @param statusCode the status code of the response
   * @param message the message associated with the response
   * @param error the error message associated with the response
   */
  public ErrorResponse(boolean success, int statusCode, String message, String error) {
    this.success = success;
    this.statusCode = statusCode;
    this.message = message;
    this.error = error;
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

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
