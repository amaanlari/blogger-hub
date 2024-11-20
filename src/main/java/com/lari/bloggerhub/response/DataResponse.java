package com.lari.bloggerhub.response;

/** This class represents the data response schema for the Blogger Hub application. */
public class DataResponse implements Response {

  private boolean success;
  private int statusCode;
  private String message;
  private Object data;

  /** Default constructor. */
  public DataResponse() {}

  /**
   * Initializes a new data response with the specified data.
   *
   * @param success the status of the response
   * @param statusCode the status code of the response
   * @param message the message associated with the response
   * @param data the data to be returned in the response
   */
  public DataResponse(boolean success, int statusCode, String message, Object data) {
    this.success = success;
    this.statusCode = statusCode;
    this.message = message;
    this.data = data;
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

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
}
