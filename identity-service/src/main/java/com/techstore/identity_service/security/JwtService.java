package com.techstore.identity_service.security;


import com.techstore.identity_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Dịch vụ chuyên xử lý JWT: Tạo token mới, giải mã token, và kiểm tra tính hợp lệ.
 */
@Service
public class JwtService {

    // Khóa bí mật dùng để ký token (Trong thực tế nên giấu ở file application.yml hoặc biến môi trường)
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    // Trích xuất Username (thường là Email) từ trong chuỗi Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Hàm generic để trích xuất một thông tin bất kỳ từ Payload của Token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Tạo Token mặc định (Giờ đây sẽ tự động nhét thêm userId vào Payload)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Ép kiểu từ UserDetails chung chung của Spring về class User thực tế của chúng ta
        if (userDetails instanceof User) {
            User customUser = (User) userDetails;
            extraClaims.put("userId", customUser.getId()); // Nhét ID vào cục hàng mang theo
        }

        return generateToken(extraClaims, userDetails);
    }

    // Tạo Token có kèm thông tin bổ sung (VD: role, phân quyền)
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis())) // Thời điểm tạo
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Thời hạn sống: 24 giờ
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Ký bằng thuật toán mã hóa HS256
                .compact();
    }

    // Kiểm tra xem Token có đúng là của User đang đăng nhập và còn hạn hay không
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Kiểm tra Token đã quá hạn (Expired) chưa
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Lấy ra ngày giờ hết hạn của Token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Dùng Secret Key để giải mã toàn bộ Payload của Token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Chuyển đổi chuỗi Secret Key dạng Base64 sang đối tượng Key của Java mã hóa
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // hàm chuyên dụng để móc userId ra từ chuỗi Token
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            // Lấy ra dưới dạng Number chung chung, vì thư viện JWT có thể tự ép nó thành Integer
            Number userId = claims.get("userId", Number.class);
            return userId != null ? userId.longValue() : null; // Đổi chuẩn sang Long
        });
    }
}