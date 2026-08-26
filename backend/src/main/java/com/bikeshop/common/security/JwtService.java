package com.bikeshop.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emissão e validação de JWT stateless. Papéis (roles) são embutidos como claim, evitando que o
 * filtro precise consultar o banco a cada requisição.
 */
@Service
public class JwtService {

  private final SecretKey signingKey;
  private final JwtProperties properties;

  public JwtService(
      @Value("${bikeshop.security.jwt.secret}") String secret, JwtProperties properties) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.properties = properties;
  }

  public String generateAccessToken(String subjectId, List<Role> roles) {
    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subjectId)
        .claim("roles", roles.stream().map(Enum::name).collect(Collectors.toList()))
        .claim("type", "access")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(properties.getAccessTokenTtlMinutes() * 60)))
        .signWith(signingKey)
        .compact();
  }

  public String generateRefreshToken(String subjectId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subjectId)
        .claim("type", "refresh")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(properties.getRefreshTokenTtlDays() * 24 * 60 * 60)))
        .signWith(signingKey)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}
