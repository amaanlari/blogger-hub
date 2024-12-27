package com.lari.bloggerhub.util.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class provides utility methods for generating, decoding, and validating JSON Web Tokens
 * (JWTs) used in the Blogger Hub application.
 *
 * <p>The class includes methods for generating access tokens and refresh tokens, as well as
 * decoding and validating tokens to extract user information.
 */
@Component
public class JwtHelper {
  static final String JWT_ISSUER = "blogger-hub";
  private static final Logger log = LoggerFactory.getLogger(JwtHelper.class);

  private final long accessTokenExpirationMinutes;
  private final long refreshTokenExpirationDays;

  private final Algorithm accessTokenAlgorithm;
  private final Algorithm refreshTokenAlgorithm;
  private final JWTVerifier accessTokenVerifier;
  private final JWTVerifier refreshTokenVerifier;

  /**
   * Constructs a new instance of the {@link JwtHelper} class with the specified JWT secret keys and
   * token expiration times.
   *
   * @param accessTokenSecret the secret key used to sign access tokens
   * @param refreshTokenSecret the secret key used to sign refresh tokens
   * @param refreshTokenExpirationDays the number of days until refresh tokens expire
   * @param accessTokenExpirationMinutes the number of minutes until access tokens expire
   */
  public JwtHelper(
      @Value("${jwt.auth.accessTokenSecret}") String accessTokenSecret,
      @Value("${jwt.auth.refreshTokenSecret}") String refreshTokenSecret,
      @Value("${jwt.auth.refreshTokenExpirationDays}") int refreshTokenExpirationDays,
      @Value("${jwt.auth.accessTokenExpirationMinutes}") int accessTokenExpirationMinutes) {
    this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    this.accessTokenAlgorithm = Algorithm.HMAC512(accessTokenSecret);
    this.refreshTokenAlgorithm = Algorithm.HMAC512(refreshTokenSecret);
    this.accessTokenVerifier = JWT.require(accessTokenAlgorithm).withIssuer(JWT_ISSUER).build();
    this.refreshTokenVerifier = JWT.require(refreshTokenAlgorithm).withIssuer(JWT_ISSUER).build();
  }

  /**
   * Generates a new access token for the specified user.
   *
   * @param user the user for whom the access token is generated
   * @return the generated access token
   */
  public String generateAccessToken(BlogUser user) {
    return JWT.create()
        .withIssuer(JWT_ISSUER)
        .withSubject(user.getId())
        .withIssuedAt(Date.from(Instant.now()))
        .withExpiresAt(
            Date.from(Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
        .sign(accessTokenAlgorithm);
  }

  /**
   * Generates a new refresh token for the specified user and refresh token entity.
   *
   * @param user the user for whom the refresh token is generated
   * @param refreshToken the refresh token entity associated with the user
   * @return the generated refresh token
   */
  public String generateRefreshToken(BlogUser user, RefreshToken refreshToken) {
    return JWT.create()
        .withIssuer(JWT_ISSUER)
        .withSubject(user.getId())
        .withClaim("tokenId", refreshToken.getId())
        .withIssuedAt(Date.from(Instant.now()))
        .withExpiresAt(Date.from(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS)))
        .sign(refreshTokenAlgorithm);
  }

  private Optional<DecodedJWT> decodeAccessToken(String token) {
    try {
      return Optional.of(accessTokenVerifier.verify(token));
    } catch (JWTVerificationException e) {
      log.error("Invalid access token", e);
    }
    return Optional.empty();
  }

  private Optional<DecodedJWT> decodeRefreshToken(String token) {
    try {
      return Optional.of(refreshTokenVerifier.verify(token));
    } catch (JWTVerificationException e) {
      log.error("Invalid refresh token", e);
    }
    return Optional.empty();
  }

  /**
   * Validates the specified access token.
   *
   * @param token the access token to validate
   * @return {@code true} if the token is valid, {@code false} otherwise
   */
  public boolean validateAccessToken(String token) {
    return decodeAccessToken(token).isPresent();
  }

  /**
   * Validates the specified refresh token.
   *
   * @param token the refresh token to validate
   * @return {@code true} if the token is valid, {@code false} otherwise
   */
  public boolean validateRefreshToken(String token) {
    return decodeRefreshToken(token).isPresent();
  }

  /**
   * Extracts the user ID from the specified access token.
   *
   * @param token the access token from which to extract the user ID
   * @return the user ID extracted from the token
   */
  public String getUserIdFromAccessToken(String token) {
    DecodedJWT decodedJwt = decodeAccessToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJwt.getSubject();
  }

  /**
   * Extracts the user ID from the specified refresh token.
   *
   * @param token the refresh token from which to extract the user ID
   * @return the user ID extracted from the token
   */
  public String getUserIdFromRefreshToken(String token) {
    DecodedJWT decodedJwt = decodeRefreshToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJwt.getSubject();
  }

  /**
   * Extracts the token ID from the specified refresh token.
   *
   * @param token the refresh token from which to extract the token ID
   * @return the token ID extracted from the token
   */
  public String getTokenIdFromRefreshToken(String token) {
    DecodedJWT decodedJwt = decodeRefreshToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJwt.getClaim("tokenId").asString();
  }
}
