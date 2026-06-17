package com.techstore.cart_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLES_CLAIM = "roles";

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(BEARER_PREFIX.length());

            // Parse & validate JWT
            Claims claims = parseJwt(jwt);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = extractUserId(claims);
                List<String> rolesList = extractRoles(claims);

                if (userId != null) {
                    List<GrantedAuthority> authorities = rolesList.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    authorities
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("JWT validation failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during JWT authentication: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parse JWT token using HS256 (HMAC with SHA-256).
     * @param token JWT token
     * @return Claims if valid, null if not valid
     */
    private Claims parseJwt(String token) {
        try {
            Key key = getSigningKey();
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("JWT validation failed: " + e.getMessage());
        }
    }

    /**
     * Extract userId from JWT claims.
     * @param claims JWT claims
     * @return userId or null if not found
     */
    private Long extractUserId(Claims claims) {
        Object userIdObj = claims.get(USER_ID_CLAIM);
        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                logger.warn("Could not parse userId claim as Long: " + userIdObj);
                return null;
            }
        }
        return null;
    }

    /**
     * Extract roles from JWT claims
     * @param claims JWT claims
     * @return List of role strings, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get(ROLES_CLAIM);
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        } else if (rolesObj instanceof String) {
            return Collections.singletonList((String) rolesObj);
        }
        return Collections.emptyList();
    }

    /**
     * Get signing key from secret (HS256 = HMAC SHA-256).
     * @return Key for HMAC SHA-256
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}