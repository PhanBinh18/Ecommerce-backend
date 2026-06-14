package com.techstore.cart_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with default settings
                .cors(Customizer.withDefaults())

                // Disable CSRF (stateless API, no form-based attacks)
                .csrf(csrf -> csrf.disable())

                // Set session management to STATELESS (JWT-based)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Allow OPTIONS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // =========== USER/GUEST CART OPERATIONS (Public, but controller checks auth/guest header) ===========
                        // GET /api/v1/carts - Get cart (user or guest)
                        .requestMatchers(HttpMethod.GET, "/api/v1/carts/**").permitAll()

                        // POST /api/v1/carts/items - Add item to cart (user or guest)
                        .requestMatchers(HttpMethod.POST, "/api/v1/carts/items").permitAll()

                        // PUT /api/v1/carts/items/{productId} - Update item quantity (user or guest)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/carts/items/**").permitAll()

                        // DELETE /api/v1/carts/items/{productId} - Remove item (user or guest)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/carts/items/**").permitAll()

                        // =========== AUTHENTICATED OPERATIONS ===========
                        // POST /api/v1/carts/merge - Merge guest cart to user (requires auth)
                        .requestMatchers(HttpMethod.POST, "/api/v1/carts/merge").authenticated()

                        // =========== INTERNAL SERVICE ENDPOINTS (Order Service) ===========
                        // DELETE /api/v1/internal/carts/clear - Clear user cart (internal, API Gateway should restrict)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/internal/carts/**").permitAll()

                        // =========== DEFAULT: Deny all other requests ===========
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}