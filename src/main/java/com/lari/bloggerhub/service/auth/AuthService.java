package com.lari.bloggerhub.service.auth;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import com.lari.bloggerhub.dto.request.auth.LoginRequestDto;
import com.lari.bloggerhub.dto.request.auth.RefreshTokenRequestDto;
import com.lari.bloggerhub.dto.request.auth.SignupRequestDto;
import com.lari.bloggerhub.dto.request.auth.UserOtpRequestDto;
import com.lari.bloggerhub.dto.response.auth.TokenResponseDto;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.RefreshTokenRepository;
import com.lari.bloggerhub.response.DataResponse;
import com.lari.bloggerhub.response.ErrorResponse;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import com.lari.bloggerhub.service.bloguser.BlogUserService;
import com.lari.bloggerhub.service.bloguser.EmailService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides methods for handling user authentication and authorization in the Blogger Hub
 * application. It allows users to log in, sign up, and generate access tokens and refresh tokens.
 */
@Service
public class AuthService {

  public static final String INVALID_TOKEN = "Invalid token";
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final EmailService emailService;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository refreshTokenRepository;
  private final BlogUserRepository blogUserRepository;
  private final JwtHelper jwtHelper;
  private final PasswordEncoder passwordEncoder;
  private final BlogUserService blogUserService;

  /**
   * Constructs a new instance of the {@link AuthService} class with the specified dependencies.
   *
   * @param authenticationManager the authentication manager for validating user credentials
   * @param refreshTokenRepository the repository for managing refresh tokens
   * @param blogUserRepository the repository for managing user accounts
   * @param jwtHelper the helper class for generating and validating JWT tokens
   * @param passwordEncoder the password encoder for hashing user passwords
   * @param blogUserService the service class for managing user-related operations
   */
  public AuthService(
      AuthenticationManager authenticationManager,
      RefreshTokenRepository refreshTokenRepository,
      BlogUserRepository blogUserRepository,
      JwtHelper jwtHelper,
      PasswordEncoder passwordEncoder,
      BlogUserService blogUserService,
      EmailService emailService) {
    this.authenticationManager = authenticationManager;
    this.refreshTokenRepository = refreshTokenRepository;
    this.blogUserRepository = blogUserRepository;
    this.jwtHelper = jwtHelper;
    this.passwordEncoder = passwordEncoder;
    this.blogUserService = blogUserService;
    this.emailService = emailService;
  }

  /**
   * This method is used to sign up a new user in the Blogger Hub application.
   *
   * @param dto the signup request data
   * @return a {@link ResponseEntity} containing the response data
   */
  @Transactional
  public ResponseEntity<Response> signup(SignupRequestDto dto) {
    log.info("Signup attempt for user: {}", dto.getUsername());
    try {
      if (blogUserRepository.existsByUsername(dto.getUsername())) {
        log.error("Username already exists: {}", dto.getUsername());
        throw new BadCredentialsException("Username already exists");
      }

      if (blogUserRepository.existsByEmail(dto.getEmail())) {
        log.error("Email already exists: {}", dto.getEmail());
        throw new BadCredentialsException("Email already exists");
      }

      dto.setPassword(passwordEncoder.encode(dto.getPassword()));

      blogUserService.createBlogUser(dto);

      BlogUser user =
          blogUserRepository
              .findByUsername(dto.getUsername())
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      RefreshToken refreshToken = new RefreshToken();
      refreshToken.setOwner(user);
      refreshTokenRepository.save(refreshToken);

      String accessToken = jwtHelper.generateAccessToken(user);
      String refreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

      return ResponseEntity.ok(
          new DataResponse(
              true,
              HttpStatus.OK.value(),
              "Signed up",
              new TokenResponseDto(user.getId(), accessToken, refreshTokenString)));
    } catch (Exception e) {
      log.error("Signup failed for user: {}", dto.getUsername());
      throw new BadCredentialsException("Invalid credentials");
    }
  }

  /**
   * This method is used to log in a user to the Blogger Hub application.
   *
   * @param dto the login request data
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<Response> login(LoginRequestDto dto) {
    log.debug("Login attempt for user: {}", dto.getUsername());
    BlogUser unauthenticatedUser =
        blogUserRepository
            .findByUsername(dto.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    if (!unauthenticatedUser.isVerified()) {
      log.error("User not verified: {}", dto.getUsername());
      emailService.sendVerificationEmail(unauthenticatedUser.getEmail());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              new ErrorResponse(false, HttpStatus.UNAUTHORIZED.value(), "User not verified", null));
    }
    try {
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

      log.info("Login successful for user: {}", dto.getUsername());
      return ResponseEntity.ok(
          new DataResponse(
              true,
              HttpStatus.OK.value(),
              "Logged in",
              new TokenResponseDto(user.getId(), accessToken, refreshTokenString)));
    } catch (Exception e) {
      log.error("Login failed for user: {}", dto.getUsername());
      throw new BadCredentialsException("Invalid credentials");
    }
  }

  /**
   * This method is used to log out a user from the Blogger Hub application.
   *
   * @param dto the refresh token request data
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<Response> logout(RefreshTokenRequestDto dto) {
    String userId = jwtHelper.getUserIdFromRefreshToken(dto.getRefreshToken());
    log.info("Logout attempt for user: {}", userId);
    try {
      String refreshTokenString = dto.getRefreshToken();
      if (jwtHelper.validateRefreshToken(refreshTokenString)
          && refreshTokenRepository.existsById(
              jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
        refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));
        log.info("Logout successful  ");
        return ResponseEntity.ok(new SuccessResponse(true, HttpStatus.OK.value(), "Logged out"));
      }
      throw new BadCredentialsException(INVALID_TOKEN);
    } catch (Exception e) {
      log.error("Logout failed  ");
      throw e;
    }
  }

  /**
   * This method is used to log out a user from all devices in the Blogger Hub application.
   *
   * @param dto the refresh token request data
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<Response> logoutAll(RefreshTokenRequestDto dto) {
    log.info("Logout all attempt");
    try {
      String refreshTokenString = dto.getRefreshToken();
      if (jwtHelper.validateRefreshToken(refreshTokenString)
          && refreshTokenRepository.existsById(
              jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
        // valid and exists in db

        refreshTokenRepository.deleteByOwner_Id(
            jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
        log.info("Logout from all devices successful");
        return ResponseEntity.ok(
            new SuccessResponse(true, HttpStatus.OK.value(), "Logged out from all"));
      }

      throw new BadCredentialsException(INVALID_TOKEN);
    } catch (Exception e) {
      log.error("Logout from all devices failed");
      throw e;
    }
  }

  /**
   * This method is used to generate an access token for a user in the Blogger Hub application.
   *
   * @param dto the refresh token request data
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<TokenResponseDto> accessToken(RefreshTokenRequestDto dto) {
    log.info("Access token generation attempt");
    try {
      String refreshTokenString = dto.getRefreshToken();
      if (jwtHelper.validateRefreshToken(refreshTokenString)
          && refreshTokenRepository.existsById(
              jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
        // valid and exists in db

        BlogUser user =
            blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
        String accessToken = jwtHelper.generateAccessToken(user);

        log.info("Access token generation successful");
        return ResponseEntity.ok(
            new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
      }

      throw new BadCredentialsException(INVALID_TOKEN);
    } catch (Exception e) {
      log.error("Access token generation failed");
      throw e;
    }
  }

  /**
   * This method is used to generate a refresh token for a user in the Blogger Hub application.
   *
   * @param dto the refresh token request data
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<TokenResponseDto> refreshToken(RefreshTokenRequestDto dto) {
    log.info("Refresh token generation attempt");
    try {
      String refreshTokenString = dto.getRefreshToken();
      if (jwtHelper.validateRefreshToken(refreshTokenString)
          && refreshTokenRepository.existsById(
              jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {

        refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));

        BlogUser user =
            blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setOwner(user);
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtHelper.generateAccessToken(user);
        String newRefreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

        log.info("Refresh token generation successful");
        return ResponseEntity.ok(
            new TokenResponseDto(user.getId(), accessToken, newRefreshTokenString));
      }

      throw new BadCredentialsException(INVALID_TOKEN);
    } catch (Exception e) {
      log.error("Refresh token generation failed");
      throw e;
    }
  }

  /**
   * This method is used to send an OTP to the user's email address for verification.
   *
   * @param otpRequestDto the user OTP request data containing the email address and OTP code to
   *     send to the user email address for verification purposes {@link UserOtpRequestDto}
   * @return a {@link ResponseEntity} containing the response data
   */
  public ResponseEntity<Response> verifyOtp(UserOtpRequestDto otpRequestDto) {
    boolean isVerified = emailService.verifyEmail(otpRequestDto.getEmail(), otpRequestDto.getOtp());
    if (isVerified) {
      return ResponseEntity.ok(new SuccessResponse(true, HttpStatus.OK.value(), "Email verified"));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
        new ErrorResponse(false, HttpStatus.UNAUTHORIZED.value(), "Invalid OTP", null));
  }
}
