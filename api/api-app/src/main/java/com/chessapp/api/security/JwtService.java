package com.chessapp.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${security.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }
  public String issue(String subject, long ttlMs) {
    return Jwts.builder().setSubject(subject)
      .setExpiration(new Date(System.currentTimeMillis()+ttlMs))
      .signWith(key, SignatureAlgorithm.HS256).compact();
  }
  public String subject(String jwt) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody().getSubject();
  }
}

