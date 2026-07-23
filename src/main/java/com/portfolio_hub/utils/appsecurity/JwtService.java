package com.portfolio_hub.utils.appsecurity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${application.security.secret-key}")
  private String secretKey;

  @Value("${application.security.access-token-minutes:1440}")
  private long accessTokenMinutes;

  @Value("${application.security.refresh-token-days:30}")
  private long refreshTokenDays;

  @PostConstruct
  void validateConfiguration() {
    if (
      secretKey == null ||
      secretKey.getBytes(StandardCharsets.UTF_8).length < 32
    ) {
      throw new IllegalStateException(
        "JWT_SECRET_KEY must be configured with at least 32 random bytes"
      );
    }
    if (accessTokenMinutes <= 0 || refreshTokenDays <= 0) {
      throw new IllegalStateException(
        "JWT token lifetimes must be greater than zero"
      );
    }
  }

  public String generateToken(String username, Map<String, Object> claims) {
    return buildToken(
      username,
      claims,
      Duration.ofMinutes(accessTokenMinutes),
      "access"
    );
  }

  public String generateRefreshToken(
    String username,
    Map<String, Object> claims
  ) {
    return buildToken(
      username,
      claims,
      Duration.ofDays(refreshTokenDays),
      "refresh"
    );
  }

  private String buildToken(
    String username,
    Map<String, Object> claims,
    Duration duration,
    String tokenType
  ) {
    Date now = new Date();
    Map<String, Object> tokenClaims = new HashMap<>(claims);
    tokenClaims.put("tokenType", tokenType);
    return Jwts.builder()
      .setClaims(tokenClaims)
      .setSubject(username)
      .setIssuedAt(now)
      .setExpiration(new Date(now.getTime() + duration.toMillis()))
      .signWith(signingKey(), SignatureAlgorithm.HS256)
      .compact();
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean isTokenValid(String token, String username) {
    return (
      username.equalsIgnoreCase(extractUsername(token)) &&
      extractClaim(token, Claims::getExpiration).after(new Date())
    );
  }

  public boolean isAccessToken(String token) {
    return "access".equals(
      extractClaim(token, claims -> claims.get("tokenType", String.class))
    );
  }

  public boolean isRefreshToken(String token) {
    return "refresh".equals(
      extractClaim(token, claims -> claims.get("tokenType", String.class))
    );
  }

  public int extractTokenVersion(String token) {
    Integer value = extractClaim(token, claims ->
      claims.get("tokenVersion", Integer.class)
    );
    return value == null ? 0 : value;
  }

  private <T> T extractClaim(String token, Function<Claims, T> resolver) {
    Claims claims = Jwts.parserBuilder()
      .setSigningKey(signingKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
    return resolver.apply(claims);
  }

  private Key signingKey() {
    byte[] bytes = secretKey.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) throw new IllegalStateException(
      "JWT secret must be at least 32 bytes"
    );
    return Keys.hmacShaKeyFor(bytes);
  }
}
