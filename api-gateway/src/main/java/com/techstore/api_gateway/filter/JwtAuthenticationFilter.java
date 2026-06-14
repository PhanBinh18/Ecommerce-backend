package com.techstore.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. CÁC ĐƯỜNG DẪN KHÔNG CẦN BẢO VỆ (ĐÃ FIX: Dùng "/auth/" để bao quát các bản v1, v2...)
        if (path.contains("/auth/") || path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/vnpay-ipn")) {
            return chain.filter(exchange);
        }

        // 2. KIỂM TRA HEADER AUTHORIZATION
        List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization format. Expected 'Bearer <token>'", HttpStatus.UNAUTHORIZED);
        }

        // 3. CẮT LẤY TOKEN VÀ XÁC THỰC
        String token = authHeader.substring(7);
        try {
            byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
            Key key = Keys.hmacShaKeyFor(keyBytes);
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);

            // XÁC THỰC THÀNH CÔNG -> Cho phép đi tiếp
            return chain.filter(exchange);

        } catch (Exception e) {
            System.out.println("====> JWT LỖI CHI TIẾT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        System.out.println("Gateway Security Blocked: " + errMessage);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}