package com.y1.community.security;

import com.y1.community.config.JwtProperties;
import com.y1.community.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties props) {
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少 32 字节（HS256 要求 256 位）");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = props.accessTtl();
        this.refreshTtl = props.refreshTtl();
    }

    public String createAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtl.toMillis()))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String createRefreshToken(User user, String jti) {
        Date now = new Date();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.getId()))
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtl.toMillis()))
                .signWith(key)
                .compact();
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}