package com.lari.bloggerhub.util.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lari.bloggerhub.model.BlogUser;
import com.lari.bloggerhub.model.RefreshToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class JwtHelper {
  static final String JWT_ISSUER = "blogger-hub";
  private static final Logger log = LoggerFactory.getLogger(JwtHelper.class);

  private final long accessTokenExpirationMs;
  private final long refreshTokenExpirationMs;

  private final Algorithm accessTokenAlgorithm;
  private final Algorithm refreshTokenAlgorithm;
  private final JWTVerifier accessTokenVerifier;
  private final JWTVerifier refreshTokenVerifier;

  // Configure accessTokenSecret, refreshTokenSecret, refreshTokenExpirationDays and
  // accessTokenExpirationMinutes in application.properties or application.yml
  public JwtHelper(
      @Value("${accessTokenSecret}") String accessTokenSecret,
      @Value("${refreshTokenSecret}") String refreshTokenSecret,
      @Value("${refreshTokenExpirationDays}") int refreshTokenExpirationDays,
      @Value("${accessTokenExpirationMinutes}") int accessTokenExpirationMinutes) {
    accessTokenExpirationMs = (long) accessTokenExpirationMinutes * 60 * 1000;
    refreshTokenExpirationMs = (long) refreshTokenExpirationDays * 24 * 60 * 60 * 1000;
    accessTokenAlgorithm = Algorithm.HMAC512(accessTokenSecret);
    refreshTokenAlgorithm = Algorithm.HMAC512(refreshTokenSecret);
    accessTokenVerifier = JWT.require(accessTokenAlgorithm).withIssuer(JWT_ISSUER).build();
    refreshTokenVerifier = JWT.require(refreshTokenAlgorithm).withIssuer(JWT_ISSUER).build();
  }

  public String generateAccessToken(BlogUser user) {
    return JWT.create()
        .withIssuer(JWT_ISSUER)
        .withSubject(user.getId())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(new Date().getTime() + accessTokenExpirationMs))
        .sign(accessTokenAlgorithm);
  }

  public String generateRefreshToken(BlogUser user, RefreshToken refreshToken) {
    return JWT.create()
        .withIssuer(JWT_ISSUER)
        .withSubject(user.getId())
        .withClaim("tokenId", refreshToken.getId())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date((new Date()).getTime() + refreshTokenExpirationMs))
        .sign(refreshTokenAlgorithm);
  }

  private Optional<DecodedJWT> decodeAccessToken(String token) {
    try {
      return Optional.of(accessTokenVerifier.verify(token));
    } catch (JWTVerificationException e) {
      log.error("invalid access token", e);
    }
    return Optional.empty();
  }

  private Optional<DecodedJWT> decodeRefreshToken(String token) {
    try {
      return Optional.of(refreshTokenVerifier.verify(token));
    } catch (JWTVerificationException e) {
      log.error("invalid refresh token", e);
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
