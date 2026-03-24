# 📝 Phân tích Kiến trúc Bảo mật: Spring Security & JWT

Tài liệu này mô tả chi tiết luồng xác thực (Authentication) và phân quyền (Authorization) không trạng thái (Stateless) sử dụng JSON Web Token (JWT). Kiến trúc này được lựa chọn để đáp ứng khả năng mở rộng và giao tiếp an toàn trong mô hình Microservices.

---

## 1. Cấu trúc của một chuỗi JWT
Trước khi hiểu luồng chạy, cần nắm rõ JWT không phải là dữ liệu bị mã hóa che giấu (encrypted), mà là dữ liệu được **ký xác nhận (signed)**. Một chuỗi JWT gồm 3 phần, cách nhau bởi dấu chấm (`.`): `Header.Payload.Signature`

* **Header:** Chứa loại token (JWT) và thuật toán ký (VD: HMAC SHA256).
* **Payload (Claims):** Chứa dữ liệu thực tế mang theo. Ví dụ: `sub: "admin@gmail.com"`, `role: "ROLE_ADMIN"`, `exp: 1700000000` (thời gian hết hạn). Ai cũng có thể decode phần này để xem, nên tuyệt đối không chứa mật khẩu ở đây.
* **Signature (Chữ ký bí mật):** Đây là phần quan trọng nhất. Nó được tạo ra bằng cách lấy `Header` + `Payload` băm cùng với một **Secret Key** (Khóa bí mật chỉ lưu trên Server). Bất kỳ ai cố tình sửa đổi Payload (VD: tự nâng quyền lên ADMIN) thì Signature sẽ bị sai lệch hoàn toàn.

---

## 2. Giai đoạn 1: Quá trình Xác thực (Authentication Flow - Login)



Giai đoạn này xảy ra khi người dùng gửi yêu cầu đăng nhập để lấy Token.

1. **Client Request:** Người dùng gửi `POST /api/auth/login` với body chứa `{email, password}`.
2. **AuthenticationManager:** Spring Security tiếp nhận request. `AuthenticationManager` chịu trách nhiệm xác thực.
3. **UserDetailsService:** `AuthenticationManager` gọi interface `UserDetailsService` (do chúng ta tự code) để truy vấn xuống Database (bảng `users`) tìm kiếm User theo `email`.
4. **PasswordEncoder:** Nếu tìm thấy User, Spring Security dùng `PasswordEncoder` (thường là BCrypt) để so sánh mật khẩu người dùng gửi lên với mật khẩu đã băm trong Database.
5. **JwtService (Generate Token):** Nếu mật khẩu khớp khớp, hệ thống xác nhận danh tính hợp lệ. Custom class `JwtService` sẽ được gọi để tạo ra chuỗi JWT. Nó nén `email`, `id`, và `role` vào Payload, sau đó dùng Secret Key để đóng dấu (tạo Signature).
6. **Response:** Trả chuỗi JWT về cho Client. Server kết thúc vòng đời request và không lưu trữ bất kỳ Session nào trên RAM (Stateless).

---

## 3. Giai đoạn 2: Quá trình Phân quyền (Authorization Flow - Access API)



Giai đoạn này xảy ra khi người dùng dùng Token để truy cập các API được bảo vệ (VD: `POST /api/products`, `GET /api/orders`).

1. **Client Request:** Client gọi API, bắt buộc đính kèm JWT vào HTTP Header:
   `Authorization: Bearer <chuỗi_jwt>`
2. **JwtAuthenticationFilter (Chốt chặn cửa ngõ):** Request đi vào hệ thống sẽ đụng trúng một màng lọc (Filter) do chúng ta tự định nghĩa (kế thừa từ `OncePerRequestFilter`).
3. **Extract & Validate:** - Filter bóc tách chuỗi JWT ra khỏi chữ "Bearer ".
    - Gọi `JwtService` dùng Secret Key để kiểm tra tính hợp lệ của chữ ký (Signature) và xem token đã hết hạn (Expired) chưa.
4. **Cấp quyền (SecurityContextHolder):** - Nếu Token hợp lệ, Filter sẽ giải mã Payload lấy ra `email` và `role`.
    - Tạo ra một đối tượng `UsernamePasswordAuthenticationToken` (chứa thông tin user và danh sách quyền hạn).
    - Đặt đối tượng này vào `SecurityContextHolder`. Đây là cách thông báo cho toàn bộ ứng dụng Spring Boot biết rằng: "Request hiện tại đang được thực hiện bởi một người dùng hợp lệ có quyền X".
5. **Controller & @PreAuthorize:** - Request đi tiếp lọt vào Controller.
    - Nếu phương thức có gắn `@PreAuthorize("hasRole('ADMIN')")`, Spring sẽ nhìn vào `SecurityContextHolder` để kiểm tra. Nếu User chỉ có quyền `ROLE_USER`, hệ thống lập tức ném ra lỗi `403 Forbidden`. Nếu đúng quyền, logic nghiệp vụ được thực thi.

---

## 4. Tại sao luồng này bắt buộc cho Microservices?

Trong Microservices, nếu sử dụng Session/Cookie truyền thống, Identity Service và Order Service không chia sẻ chung vùng nhớ (RAM). User đăng nhập ở Identity Service thì Order Service sẽ không nhận diện được.

Với JWT, mọi thứ được giải quyết triệt để theo cơ chế **Phi tập trung (Decentralized Validation)**:
- **Identity Service:** Là nơi duy nhất có kết nối với bảng `users` để kiểm tra mật khẩu và in ra JWT.
- **Product/Order Service:** Hoàn toàn không cần kết nối bảng `users`. Chúng chỉ cần giữ bản sao của **Secret Key**. Khi nhận được request kẹp JWT, chúng tự lấy Secret Key ra kiểm tra chữ ký. Nếu chữ ký đúng, chúng tin tưởng tuyệt đối vào thông tin `role` và `userId` ghi trong Payload mà không cần gọi mạng sang hỏi Identity Service.