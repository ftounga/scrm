package com.isp.scrm.service;

import com.isp.scrm.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateTechnicalToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(props.getSubject())
                .issuer(props.getIssuer())
                .claim("type", props.getType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getExpirationDays(), ChronoUnit.DAYS)))
                .signWith(key())   // plus besoin de SignatureAlgorithm
                .compact();
    }

    public Jws<Claims> validate(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();

        if (!props.getIssuer().equals(claims.getIssuer())) {
            throw new JwtException("Invalid issuer");
        }
        if (!props.getSubject().equals(claims.getSubject())) {
            throw new JwtException("Invalid subject");
        }

        String type = claims.get("type", String.class);
        if (!props.getType().equals(type)) {
            throw new JwtException("Invalid token type");
        }

        return jws;
    }
}
