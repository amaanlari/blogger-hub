package com.lari.bloggerhub.controller.auth;

import com.lari.bloggerhub.dto.request.BlogUserRequestDto;
import com.lari.bloggerhub.dto.request.auth.LoginRequestDto;
import com.lari.bloggerhub.dto.response.TokenResponseDto;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.RefreshTokenRepository;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import com.lari.bloggerhub.service.BlogUserService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
  public static final String INVALID_TOKEN = "Invalid token";

  AuthenticationManager authenticationManager;
  RefreshTokenRepository refreshTokenRepository;
  BlogUserRepository blogUserRepository;
  JwtHelper jwtHelper;
  PasswordEncoder passwordEncoder;
  BlogUserService blogUserService;

  /**
   * Constructs a new instance of the {@link AuthController} class with the specified dependencies.
   *
   * @param authenticationManager the authentication manager for validating user credentials
   * @param refreshTokenRepository the repository for managing refresh tokens
   * @param userRepository the repository for managing user data
   * @param jwtHelper the helper class for generating and validating JWT tokens
   * @param passwordEncoder the encoder for hashing user passwords
   * @param userService the service class for managing user-related operations
   */
  public AuthController(
      AuthenticationManager authenticationManager,
      RefreshTokenRepository refreshTokenRepository,
      BlogUserRepository userRepository,
      JwtHelper jwtHelper,
      PasswordEncoder passwordEncoder,
      BlogUserService userService) {
    this.authenticationManager = authenticationManager;
    this.refreshTokenRepository = refreshTokenRepository;
    this.blogUserRepository = userRepository;
    this.jwtHelper = jwtHelper;
    this.passwordEncoder = passwordEncoder;
    this.blogUserService = userService;
  }

  /**
   * Logs in a user with the specified username and password. If the credentials are valid, the
   * method generates an access token and a refresh token for the user.
   *
   * @param dto the login request containing the user's credentials
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("/login")
  public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    BlogUser user = (BlogUser) authentication.getPrincipal();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setOwner(user);
    refreshTokenRepository.save(refreshToken);

    String accessToken = jwtHelper.generateAccessToken(user);
    String refreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

    return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
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
  public ResponseEntity<TokenResponseDto> signup(@RequestBody BlogUserRequestDto dto) {
    BlogUser user = new BlogUser();
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setBio(dto.getBio());
    user.setProfilePicture(dto.getProfilePicture());
    blogUserRepository.save(user);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setOwner(user);
    refreshTokenRepository.save(refreshToken);

    String accessToken = jwtHelper.generateAccessToken(user);
    String refreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

    return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
  }

  /**
   * Logs out a user by deleting the specified refresh token from the database.
   *
   * @param dto the token response containing the refresh token to delete
   * @return a response entity indicating the outcome of the logout operation
   */
  @PostMapping("logout")
  public ResponseEntity<Response> logout(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db
      refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));
      return ResponseEntity.ok(new SuccessResponse(true, HttpStatus.OK.value(), "Logged out"));
    }

    throw new BadCredentialsException(INVALID_TOKEN);
  }

  /**
   * Logs out a user from all devices by deleting all refresh tokens associated with the user from
   * the database.
   *
   * @param dto the token response containing the refresh token to delete
   * @return a response entity indicating the outcome of the logout operation
   */
  @PostMapping("logout-all")
  public ResponseEntity<Response> logoutAll(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      refreshTokenRepository.deleteByOwner_Id(
          jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
      return ResponseEntity.ok(
          new SuccessResponse(true, HttpStatus.OK.value(), "Logged out from all"));
    }

    throw new BadCredentialsException(INVALID_TOKEN);
  }

  /**
   * Generates a new access token for the user with the specified refresh token. If the refresh
   * token is valid, the method generates a new access token and returns it to the user.
   *
   * @param dto the token response containing the refresh token to use
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("access-token")
  public ResponseEntity<TokenResponseDto> accessToken(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      BlogUser user =
          blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
      String accessToken = jwtHelper.generateAccessToken(user);

      return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
    }

    throw new BadCredentialsException(INVALID_TOKEN);
  }

  /**
   * Generates a new access token and refresh token for the user with the specified refresh token.
   * If the refresh token is valid, the method generates new tokens and returns them to the user.
   *
   * @param dto the token response containing the refresh token to use
   * @return a response entity containing the user's ID, access token, and refresh token
   */
  @PostMapping("refresh-token")
  public ResponseEntity<TokenResponseDto> refreshToken(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));

      BlogUser user =
          blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));

      RefreshToken refreshToken = new RefreshToken();
      refreshToken.setOwner(user);
      refreshTokenRepository.save(refreshToken);

      String accessToken = jwtHelper.generateAccessToken(user);
      String newRefreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

      return ResponseEntity.ok(
          new TokenResponseDto(user.getId(), accessToken, newRefreshTokenString));
    }

    throw new BadCredentialsException(INVALID_TOKEN);
  }
}
