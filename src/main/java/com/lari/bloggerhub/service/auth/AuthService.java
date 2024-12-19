package com.lari.bloggerhub.service.auth;

import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import com.lari.bloggerhub.dto.request.auth.LoginRequestDto;
import com.lari.bloggerhub.dto.request.auth.RefreshTokenRequestDto;
import com.lari.bloggerhub.dto.request.auth.SignupRequestDto;
import com.lari.bloggerhub.dto.response.auth.TokenResponseDto;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.RefreshTokenRepository;
import com.lari.bloggerhub.response.Response;
import com.lari.bloggerhub.response.SuccessResponse;
import com.lari.bloggerhub.service.bloguser.BlogUserService;
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

@Service
public class AuthService {

  public static final String INVALID_TOKEN = "Invalid token";
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  AuthenticationManager authenticationManager;
  RefreshTokenRepository refreshTokenRepository;
  BlogUserRepository blogUserRepository;
  JwtHelper jwtHelper;
  PasswordEncoder passwordEncoder;
  BlogUserService blogUserService;

  public AuthService(
      AuthenticationManager authenticationManager,
      RefreshTokenRepository refreshTokenRepository,
      BlogUserRepository blogUserRepository,
      JwtHelper jwtHelper,
      PasswordEncoder passwordEncoder,
      BlogUserService blogUserService) {
    this.authenticationManager = authenticationManager;
    this.refreshTokenRepository = refreshTokenRepository;
    this.blogUserRepository = blogUserRepository;
    this.jwtHelper = jwtHelper;
    this.passwordEncoder = passwordEncoder;
    this.blogUserService = blogUserService;
  }

  public ResponseEntity<TokenResponseDto> login(LoginRequestDto dto) {
    log.info("Login attempt for user: {}", dto.getUsername());
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
      return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
    } catch (Exception e) {
      log.error("Login failed for user: {}", dto.getUsername());
      throw new BadCredentialsException("Invalid credentials");
    }
  }

  @Transactional
  public ResponseEntity<TokenResponseDto> signup(SignupRequestDto dto) {
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

      return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
    } catch (Exception e) {
      log.error("Signup failed for user: {}", dto.getUsername());
      throw new BadCredentialsException("Invalid credentials");
    }
  }

  public ResponseEntity<Response> logout(RefreshTokenRequestDto dto) {
    log.info(
        "Logout attempt for user: {}", jwtHelper.getTokenIdFromRefreshToken(dto.getRefreshToken()));
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
}
