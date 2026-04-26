package com.techstore.identity_service.service;


import com.techstore.identity_service.dto.AuthResponse;
import com.techstore.identity_service.dto.LoginRequest;
import com.techstore.identity_service.dto.RegisterRequest;
import com.techstore.identity_service.entity.User;
import com.techstore.identity_service.repository.UserRepository;
import com.techstore.identity_service.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Xử lý đăng ký: Tạo User mới, băm mật khẩu, lưu DB và trả về Token.
     */
    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        // Băm mật khẩu trước khi lưu vào Database
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        // Mặc định role là ROLE_USER, bạn có thể tạo 1 API riêng hoặc sửa trực tiếp trong DB để cấp quyền ROLE_ADMIN

        userRepository.save(user);

        // Tạo JWT Token cho User vừa đăng ký
        String jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole()) // Lưu ý: Nếu Role của bạn là Enum thì dùng .name(), nếu là kiểu String thì chỉ cần .getRole()
                .build();
    }

    /**
     * Xử lý đăng nhập: Kiểm tra Email/Password, nếu đúng thì cấp Token.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Gọi cỗ máy AuthenticationManager của Spring Security ra để kiểm tra
        // Nếu sai mật khẩu hoặc email, nó sẽ tự động ném ra lỗi (Exception) và dừng lại ở đây
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Nếu đi qua được bước 1 nghĩa là đăng nhập thành công. Ta lấy thông tin User từ DB lên.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        // 3. In thẻ JWT và trả về
        String jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole()) // Tương tự như trên
                .build();
    }
}