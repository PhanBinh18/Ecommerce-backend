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
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // SWAGGER
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // =========== USER/GUEST CART OPERATIONS (Public, but controller checks auth/guest header) ===========
                        .requestMatchers(HttpMethod.GET, "/api/v1/carts/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/carts/items").permitAll()

                        .requestMatchers(HttpMethod.PUT, "/api/v1/carts/items/**").permitAll()

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/carts/items/**").permitAll()

                        // =========== AUTHENTICATED OPERATIONS ===========
                        .requestMatchers(HttpMethod.POST, "/api/v1/carts/merge").authenticated()

                        // =========== INTERNAL SERVICE ENDPOINTS (Order Service) ===========
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/internal/carts/**").permitAll()

                        // =========== DEFAULT: Deny all other requests ===========
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}