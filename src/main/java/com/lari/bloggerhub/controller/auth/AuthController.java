package com.lari.bloggerhub.controller.auth;

import com.lari.bloggerhub.model.BlogUser;
import com.lari.bloggerhub.model.RefreshToken;
import com.lari.bloggerhub.dto.request.BlogUserRequestDto;
import com.lari.bloggerhub.dto.request.auth.LoginRequestDto;
import com.lari.bloggerhub.dto.response.TokenResponseDto;
import com.lari.bloggerhub.repository.BlogUserRepository;
import com.lari.bloggerhub.repository.RefreshTokenRepository;
import com.lari.bloggerhub.service.BlogUserService;
import com.lari.bloggerhub.util.jwt.JwtHelper;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  AuthenticationManager authenticationManager;
  RefreshTokenRepository refreshTokenRepository;
  BlogUserRepository blogUserRepository;
  JwtHelper jwtHelper;
  PasswordEncoder passwordEncoder;
  BlogUserService blogUserService;

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

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto dto) {
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

  @PostMapping("/signup")
  public ResponseEntity<?> signup( @RequestBody BlogUserRequestDto dto) {
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

  @PostMapping("logout")
  public ResponseEntity<?> logout(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db
      refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));
      return ResponseEntity.ok().build();
    }

    throw new BadCredentialsException("invalid token");
  }

  @PostMapping("logout-all")
  public ResponseEntity<?> logoutAll(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      refreshTokenRepository.deleteByOwner_Id(
          jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
      return ResponseEntity.ok().build();
    }

    throw new BadCredentialsException("invalid token");
  }

  @PostMapping("access-token")
  public ResponseEntity<?> accessToken(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      BlogUser user = blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));
      String accessToken = jwtHelper.generateAccessToken(user);

      return ResponseEntity.ok(new TokenResponseDto(user.getId(), accessToken, refreshTokenString));
    }

    throw new BadCredentialsException("invalid token");
  }

  @PostMapping("refresh-token")
  public ResponseEntity<?> refreshToken(@RequestBody TokenResponseDto dto) {
    String refreshTokenString = dto.getRefreshToken();
    if (jwtHelper.validateRefreshToken(refreshTokenString)
        && refreshTokenRepository.existsById(
            jwtHelper.getTokenIdFromRefreshToken(refreshTokenString))) {
      // valid and exists in db

      refreshTokenRepository.deleteById(jwtHelper.getTokenIdFromRefreshToken(refreshTokenString));

      BlogUser user = blogUserService.findById(jwtHelper.getUserIdFromRefreshToken(refreshTokenString));

      RefreshToken refreshToken = new RefreshToken();
      refreshToken.setOwner(user);
      refreshTokenRepository.save(refreshToken);

      String accessToken = jwtHelper.generateAccessToken(user);
      String newRefreshTokenString = jwtHelper.generateRefreshToken(user, refreshToken);

      return ResponseEntity.ok(
          new TokenResponseDto(user.getId(), accessToken, newRefreshTokenString));
    }

    throw new BadCredentialsException("invalid token");
  }
}
