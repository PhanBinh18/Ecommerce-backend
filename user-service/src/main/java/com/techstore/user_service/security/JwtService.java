package com.techstore.user_service.security;

import com.techstore.user_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Dịch vụ xử lý JWT và blacklist token trên Redis.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private final StringRedisTemplate redisTemplate;

    public JwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof User) {
            User customUser = (User) userDetails;
            extraClaims.put("userId", customUser.getId());
        }
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)) // 24h
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()))
                && !isTokenExpired(token)
                && !isTokenBlacklisted(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        // 🟢 ĐÃ FIX: Truyền đúng tên biến secretKey viết thường
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Number userId = claims.get("userId", Number.class);
            return userId != null ? userId.longValue() : null;
        });
    }

    // --------------------------
    // Redis-based blacklist API
    // --------------------------

    /**
     * Blacklist a token by storing its signature part into Redis with TTL equal to remaining life.
     * Key: auth:blacklist:{token_signature}
     */
    public void blacklistToken(String token) {
        long remaining = getTokenRemainingMillis(token);
        if (remaining <= 0) return; // already expired
        String sig = extractTokenSignature(token);
        String key = getBlacklistKey(sig);
        // store a simple marker value
        redisTemplate.opsForValue().set(key, "blacklisted", remaining, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        String sig = extractTokenSignature(token);
        String key = getBlacklistKey(sig);
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    private String getBlacklistKey(String signature) {
        return "auth:blacklist:" + signature;
    }

    private String extractTokenSignature(String token) {
        if (token == null) return "";
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            return parts[2];
        }
        // fallback to using full token (less ideal)
        return token;
    }

    /**
     * Compute remaining milliseconds until token expiration.
     */
    public long getTokenRemainingMillis(String token) {
        try {
            Date exp = extractExpiration(token);
            long remaining = exp.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception ex) {
            return 0;
        }
    }
}