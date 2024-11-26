package com.lari.bloggerhub.util.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lari.bloggerhub.document.BlogUser;
import com.lari.bloggerhub.document.RefreshToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

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

  public String generateAccessToken(BlogUser user) {
    return JWT.create()
        .withIssuer(JWT_ISSUER)
        .withSubject(user.getId())
        .withIssuedAt(Date.from(Instant.now()))
        .withExpiresAt(
            Date.from(Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
        .sign(accessTokenAlgorithm);
  }

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

  public boolean validateAccessToken(String token) {
    return decodeAccessToken(token).isPresent();
  }

  public boolean validateRefreshToken(String token) {
    return decodeRefreshToken(token).isPresent();
  }

  public String getUserIdFromAccessToken(String token) {
    DecodedJWT decodedJWT = decodeAccessToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJWT.getSubject();
  }

  public String getUserIdFromRefreshToken(String token) {
    DecodedJWT decodedJWT = decodeRefreshToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJWT.getSubject();
  }

  public String getTokenIdFromRefreshToken(String token) {
    DecodedJWT decodedJWT = decodeRefreshToken(token).orElseThrow(IllegalArgumentException::new);
    return decodedJWT.getClaim("tokenId").asString();
  }
}
