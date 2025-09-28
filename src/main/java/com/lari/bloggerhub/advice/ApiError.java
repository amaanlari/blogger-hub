package com.lari.bloggerhub.advice;

import org.springframework.http.HttpStatus;
import java.util.List;

public class ApiError {

  private HttpStatus status;
  private String message;
  private List<String> subErrors;

  ApiError(HttpStatus status, String message, List<String> subErrors) {
    this.status = status;
    this.message = message;
    this.subErrors = subErrors;
  }

  public static ApiErrorBuilder builder() {
    return new ApiErrorBuilder();
  }

  public HttpStatus getStatus() {
    return this.status;
  }

  public String getMessage() {
    return this.message;
  }

  public List<String> getSubErrors() {
    return this.subErrors;
  }

  public void setStatus(HttpStatus status) {
    this.status = status;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setSubErrors(List<String> subErrors) {
    this.subErrors = subErrors;
  }

  public String toString() {
    return "ApiError(status="
        + this.getStatus()
        + ", message="
        + this.getMessage()
        + ", subErrors="
        + this.getSubErrors()
        + ")";
  }

  public static class ApiErrorBuilder {
    private HttpStatus status;
    private String message;
    private List<String> subErrors;

    ApiErrorBuilder() {}

    public ApiErrorBuilder status(HttpStatus status) {
      this.status = status;
      return this;
    }

    public ApiErrorBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ApiErrorBuilder subErrors(List<String> subErrors) {
      this.subErrors = subErrors;
      return this;
    }

    public ApiError build() {
      return new ApiError(this.status, this.message, this.subErrors);
    }

    public String toString() {
      return "ApiError.ApiErrorBuilder(status="
          + this.status
          + ", message="
          + this.message
          + ", subErrors="
          + this.subErrors
          + ")";
    }
  }
}
