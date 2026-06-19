package com.techstore.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT filter that now also checks Redis blacklist.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/users/auth/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // Check blacklist: if token was blacklisted (logout), reject immediately
        try {
            if (jwtService.isTokenBlacklisted(jwt)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }
        } catch (Exception ex) {
            // If Redis error or parsing error, fall through to validate token normally (or you can choose to error)
            // For now, we log to console and continue
            System.err.println("Error checking token blacklist: " + ex.getMessage());
        }

        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            System.err.println("Token đã hết hạn: " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token da het han. Vui long dang nhap lai.");
            return;
        } catch (Exception ex) {
            System.err.println("Token không hợp lệ: " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token khong hop le.");
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                System.out.println("Quyền của User đang gọi API là: " + userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Token invalid or expired — we do not set authentication. Optionally we can send 401:
                // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                // return;
            }
        }

        filterChain.doFilter(request, response);
    }
}