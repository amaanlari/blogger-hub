package com.lari.bloggerhub.controller.auth;

import com.lari.bloggerhub.dto.request.auth.LoginRequestDto;
import com.lari.bloggerhub.dto.request.auth.RefreshTokenRequestDto;
import com.lari.bloggerhub.dto.request.auth.SignupRequestDto;
import com.lari.bloggerhub.dto.request.auth.UserOtpRequestDto;
import com.lari.bloggerhub.dto.response.auth.TokenResponseDto;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class defines the REST API endpoints for handling user authentication in the Blogger Hub
 * application.
 *
 * <p>The endpoints allow users to log in, sign up, and manage their authentication tokens for
 * accessing the application's resources.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  AuthService authService;

  /**
   * Initializes a new authentication controller with the specified dependencies.
   *
   * @param authService the authentication service to use for handling user authentication
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Registers a new user in the Blogger Hub application with the specified user details. If the
   * registration is successful, the method generates an access token and a refresh token for the
   * user.
   *
   * @param dto the user details to register
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("/signup")
  public ResponseEntity<Response> signup(@RequestBody SignupRequestDto dto) {
    return authService.signup(dto);
  }

  /**
   * Logs in a user with the specified username and password. If the credentials are valid, the
   * method generates an access token and a refresh token for the user.
   *
   * @param dto the login request containing the user's credentials
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("/login")
  public ResponseEntity<Response> login(@Valid @RequestBody LoginRequestDto dto) {
    return authService.login(dto);
  }

  /**
   * Logs out a user by deleting the specified refresh token from the database.
   *
   * @param dto the token response containing the refresh token to delete
   * @return a response entity indicating the outcome of the logout operation
   */
  @PostMapping("logout")
  public ResponseEntity<Response> logout(@RequestBody RefreshTokenRequestDto dto) {
    return authService.logout(dto);
  }

  /**
   * Logs out a user from all devices by deleting all refresh tokens associated with the user from
   * the database.
   *
   * @param dto the token response containing the refresh token to delete
   * @return a response entity indicating the outcome of the logout operation
   */
  @PostMapping("logout-all")
  public ResponseEntity<Response> logoutAll(@RequestBody RefreshTokenRequestDto dto) {
    return authService.logoutAll(dto);
  }

  /**
   * Generates a new access token for the user with the specified refresh token. If the refresh
   * token is valid, the method generates a new access token and returns it to the user.
   *
   * @param dto the token response containing the refresh token to use
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("access-token")
  public ResponseEntity<TokenResponseDto> accessToken(@RequestBody RefreshTokenRequestDto dto) {
    return authService.accessToken(dto);
  }

  /**
   * Generates a new access token and refresh token for the user with the specified refresh token.
   * If the refresh token is valid, the method generates new tokens and returns them to the user.
   *
   * @param dto the token response containing the refresh token to use
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("refresh-token")
  public ResponseEntity<TokenResponseDto> refreshToken(@RequestBody RefreshTokenRequestDto dto) {
    return authService.refreshToken(dto);
  }

  @PostMapping("verify-otp")
  public ResponseEntity<Response> verifyOtp(@RequestBody UserOtpRequestDto otpRequestDto) {
    return authService.verifyOtp(otpRequestDto);
  }
}
