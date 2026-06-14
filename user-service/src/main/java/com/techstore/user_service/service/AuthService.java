package com.techstore.user_service.service;

import com.techstore.user_service.dto.AuthResponse;
import com.techstore.user_service.dto.LoginRequest;
import com.techstore.user_service.dto.RegisterRequest;
import com.techstore.user_service.entity.Role;
import com.techstore.user_service.entity.User;
import com.techstore.user_service.repository.RoleRepository;
import com.techstore.user_service.repository.UserRepository;
import com.techstore.user_service.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AuthService: register/login/googleLogout + logout (blacklist token).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

    // Google tokeninfo endpoint (simple verification)
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Register new user with default role (ROLE_CUSTOMER).
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setActive(true);

        // Ensure default role exists
        Role defaultRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));
        user.getRoles().add(defaultRole);

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return buildAuthResponse(user, jwtToken);
    }

    /**
     * Login with email/password
     */
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Optional: merge cart here (call Cart service) — TODO

        String jwtToken = jwtService.generateToken(user);
        return buildAuthResponse(user, jwtToken);
    }

    /**
     * Google login: accept id_token (from client), validate with Google,
     * create or update user, then return AuthResponse with JWT.
     *
     * Simplest verification uses Google's tokeninfo endpoint.
     */
    @SuppressWarnings("unchecked")
    public AuthResponse googleLogin(String idToken) {
        Map<String, String> tokenInfo;
        try {
            // tokeninfo returns 200 if valid; body contains fields like email, email_verified, name, picture, sub...
            tokenInfo = restTemplate.getForObject(GOOGLE_TOKENINFO_URL, Map.class, idToken);
        } catch (RestClientException ex) {
            throw new RuntimeException("Google token invalid or network error: " + ex.getMessage());
        }

        if (tokenInfo == null || tokenInfo.get("email") == null) {
            throw new RuntimeException("Không lấy được email từ Google token");
        }

        String email = tokenInfo.get("email");
        String fullName = tokenInfo.getOrDefault("name", null);
        String emailVerified = tokenInfo.getOrDefault("email_verified", "false");

        if (!"true".equalsIgnoreCase(emailVerified) && !"1".equals(emailVerified)) {
            // Depending on your policy you may still allow unverified emails; here we require verified.
            throw new RuntimeException("Email từ Google chưa được xác thực");
        }

        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            // create random password (not used) — still store hashed
            u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            u.setFullName(fullName);
            u.setPhone(null);
            u.setActive(true);

            // assign default role
            Role defaultRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));
            u.getRoles().add(defaultRole);

            return userRepository.save(u);
        });

        // If user exists, optionally update profile fields from Google (e.g., fullName) — do not overwrite phone
        if (fullName != null && (user.getFullName() == null || user.getFullName().isEmpty())) {
            user.setFullName(fullName);
            userRepository.save(user);
        }

        String jwtToken = jwtService.generateToken(user);
        return buildAuthResponse(user, jwtToken);
    }

    /**
     * Logout: blacklist token by delegating to JwtService (stores signature key in Redis).
     * Input token must be raw JWT (without "Bearer ").
     */
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        jwtService.blacklistToken(rawToken);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        // Join roles into a comma-separated string for the current AuthResponse DTO shape
        String roles = user.getRoles() == null || user.getRoles().isEmpty()
                ? ""
                : user.getRoles().stream().map(Role::getName).collect(Collectors.joining(","));

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(roles)
                .build();
    }
}