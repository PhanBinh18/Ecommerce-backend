package com.example.demo.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Trái tim của hệ thống bảo mật. Nơi định nghĩa các luật lệ truy cập và cắm màng lọc JWT vào luồng xử lý.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Bật tính năng phân quyền bằng Annotation (@PreAuthorize) trên các Controller
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Cung cấp bộ xác thực: Dạy Spring dùng UserDetailsService và BCrypt của chúng ta
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Truyền thẳng userDetailsService vào bên trong Constructor
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        // Vẫn set PasswordEncoder bằng hàm setter như bình thường
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    // 2. Cung cấp Người quản lý xác thực (Sẽ dùng trong AuthController để gọi hàm Login)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 3. Chuỗi màng lọc bảo mật (Nội quy tòa nhà)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. THÊM DÒNG NÀY: Nói với Security hãy dùng cấu hình CorsConfig đã viết
                .cors(Customizer.withDefaults())
                // Tắt CSRF vì chúng ta dùng Token, không dùng Cookie nên không sợ tấn công giả mạo request
                .csrf(AbstractHttpConfigurer::disable)

                // Cấu hình luật truy cập cho từng đường dẫn (URL)
                .authorizeHttpRequests(auth -> auth
                        // 2. THÊM DÒNG NÀY: Mở cửa thả ga cho mọi request dò đường OPTIONS của trình duyệt
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Thả cửa tự do cho các API đăng nhập, đăng ký
                        .requestMatchers("/api/auth/**").permitAll()

                        // Khách hàng vãng lai (chưa đăng nhập) vẫn được phép xem danh sách sản phẩm
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // CHỈ ADMIN mới được phép Thêm, Sửa, Xóa sản phẩm
                        .requestMatchers("/api/products/**").hasRole("ADMIN")

                        // Tất cả các API còn lại (như /api/carts, /api/orders) ĐỀU PHẢI có Token hợp lệ
                        .anyRequest().authenticated()
                )

                // Ép Spring Security chạy ở chế độ Stateless (Không lưu trạng thái đăng nhập trên RAM server)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Cắm bộ xác thực vào
                .authenticationProvider(authenticationProvider())

                // QUAN TRỌNG NHẤT: Cắm màng lọc JWT của chúng ta lên TRƯỚC màng lọc kiểm tra Username/Password mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}