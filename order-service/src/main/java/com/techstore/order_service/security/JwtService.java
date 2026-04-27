package com.techstore.order_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.function.Function;

@Service
public class JwtService {

    // BẮT BUỘC PHẢI GIỐNG HỆT IDENTITY SERVICE
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Móc userId ra từ Payload
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Number userId = claims.get("userId", Number.class);
            return userId != null ? userId.longValue() : null;
        });
    }

    // Kiểm tra tính hợp lệ (Chỉ cần kiểm tra chữ ký và hạn sử dụng)
    // Nếu token bị fake chữ ký hoặc hết hạn, hàm extractAllClaims sẽ tự văng Exception
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}