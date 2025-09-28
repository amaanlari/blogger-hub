package com.lari.bloggerhub.advice;

import com.lari.bloggerhub.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(
      ResourceNotFoundException e) {
    ApiError apiError =
        ApiError.builder().status(HttpStatus.NOT_FOUND).message(e.getMessage()).build();
    return buildErrorResponseEntity(apiError);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    ApiError apiError =
        ApiError.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).message(e.getMessage()).build();
    return buildErrorResponseEntity(apiError);
  }

  private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError) {
    return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
  }
}
