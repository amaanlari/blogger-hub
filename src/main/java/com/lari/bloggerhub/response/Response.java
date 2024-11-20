package com.lari.bloggerhub.response;

/**
 * This interface represents the response returned by the Blogger Hub application.
 *
 * <p>It is implemented by classes that represent specific types of responses, such as success
 * responses, error responses, or data responses.
 */
public interface Response {
  /**
   * Returns the status code of the response.
   *
   * @return the status code
   */
  int getStatusCode();

  /**
   * Returns the message associated with the response.
   *
   * @return the message
   */
  String getMessage();
}
