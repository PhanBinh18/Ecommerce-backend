package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Cho phép tất cả các API bắt đầu bằng /api/
                .allowedOrigins(
                        "http://localhost:3000", // Cổng mặc định của React (Create React App)
                        "http://localhost:5173", // Cổng mặc định của React (Vite) / Vue
                        "http://localhost:5500"  // Cổng của Live Server (HTML/JS thuần)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Các method được phép
                .allowedHeaders("*") // Cho phép mọi loại Header (rất quan trọng khi gửi token sau này)
                .allowCredentials(true); // Cho phép gửi cookie/thông tin xác thực
    }
}