package com.lari.bloggerhub.advice;

import java.time.LocalDateTime;
import java.util.StringJoiner;

public class ApiResponse<T> {

  private LocalDateTime timestamp;
  private T data;
  private ApiError error;

  public ApiResponse() {
    this.timestamp = LocalDateTime.now();
  }

  public ApiResponse(T data) {
    this();
    this.data = data;
  }

  public ApiResponse(ApiError error) {
    this();
    this.error = error;
  }

  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  public T getData() {
    return this.data;
  }

  public ApiError getError() {
    return this.error;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public void setData(T data) {
    this.data = data;
  }

  public void setError(ApiError error) {
    this.error = error;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ApiResponse.class.getSimpleName() + "[", "]")
        .add("timestamp=" + timestamp)
        .add("data=" + data)
        .add("error=" + error)
        .toString();
  }
}
