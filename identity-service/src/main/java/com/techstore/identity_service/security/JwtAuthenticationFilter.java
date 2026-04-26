package com.techstore.identity_service.security;

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
 * Màng lọc chặn mọi request ở cửa ngõ để kiểm tra JWT.
 * Kế thừa OncePerRequestFilter đảm bảo mỗi request chỉ bị kiểm tra đúng 1 lần.
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
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy chuỗi Token từ Header của HTTP Request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Nếu Request không có thẻ Authorization hoặc thẻ không bắt đầu bằng "Bearer ", bỏ qua không thèm soi
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Cắt bỏ chữ "Bearer " (7 ký tự đầu) để lấy chuỗi Token nguyên bản
        jwt = authHeader.substring(7);

        // 3. Gọi máy quét giải mã Token để lấy Email người dùng
        userEmail = jwtService.extractUsername(jwt);

        // 4. Nếu đọc được Email và người dùng này chưa được cấp quyền trong bối cảnh hiện tại
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Lấy thông tin User từ Database (hoặc Cache)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Kiểm tra xem Token có bị làm giả hay hết hạn không
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 6. Token chuẩn! Tạo "Thẻ thông hành" chứa thông tin và quyền hạn (Role) của User
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Không cần kiểm tra mật khẩu ở đây nữa
                        userDetails.getAuthorities()
                );

                // Gắn thêm thông tin phụ trợ (như địa chỉ IP của người dùng)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                System.out.println("Quyền của User đang gọi API là: " + userDetails.getAuthorities());

                // 7. Cấp thẻ thông hành: Đưa vào SecurityContext để các Controller phía sau biết ai đang truy cập
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Kéo rào lên, cho phép Request đi tiếp vào các màng lọc khác hoặc tiến thẳng vào Controller
        filterChain.doFilter(request, response);
    }
}