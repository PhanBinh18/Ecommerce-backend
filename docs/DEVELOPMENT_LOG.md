# NHẬT KÝ PHÁT TRIỂN BACKEND
**Dự án:** Hệ thống E-commerce thiết bị điện tử (Laptop, Màn hình, Bàn phím, Chuột)
**Kiến trúc:** Modular Monolith (Hướng tới Microservices)
**Giai đoạn:** 1 - Xây dựng Core Framework, Nghiệp vụ nền tảng & Bảo mật

---

## 1. Mục tiêu chức năng
Xây dựng một hệ thống backend thương mại điện tử chuyên bán đồ điện tử. Mục tiêu cốt lõi là các module phải hoạt động hoàn toàn độc lập (Decoupled), giao tiếp với nhau qua Service Layer thay vì Database (No Foreign Key constraints giữa các bounded contexts), tạo tiền đề vững chắc để dễ dàng phân tách thành Microservices ở Giai đoạn 2. Hệ thống được bảo mật nghiêm ngặt bằng JWT chuẩn Stateless.

## 2. User flow / Luồng xử lý
* **Luồng Xác thực & Phân quyền (MỚI):**
  * Đăng ký tài khoản (Mật khẩu được băm 1 chiều bằng BCrypt).
  * Đăng nhập -> Hệ thống cấp phát thẻ thông hành JWT (chứa thông tin Email, Role và UserId).
  * Các API nội bộ tự động trích xuất định danh người dùng từ JWT qua `SecurityContextHolder`, loại bỏ hoàn toàn việc tin tưởng `userId` từ phía Frontend.
* **Luồng Admin (Quản trị viên - Yêu cầu Token `ROLE_ADMIN`):**
  * Thêm, sửa, xóa (Soft Delete) sản phẩm -> Tải ảnh lên Cloudinary.
  * Xem danh sách sản phẩm sắp hết hàng.
  * Cập nhật trạng thái đơn hàng (PENDING -> PROCESSING -> SHIPPING -> DELIVERED) và xử lý Hủy đơn (CANCELLED) kèm tự động hoàn kho.
* **Luồng Khách hàng (User - Yêu cầu Token `ROLE_USER` hoặc khách vãng lai):**
  * Khách vãng lai: Xem danh sách sản phẩm, tìm kiếm, lọc theo Danh mục, Hãng và Sắp xếp động.
  * Khách đã đăng nhập: Thêm sản phẩm vào Giỏ hàng của chính mình.
  * **Thanh toán (Checkout):** Hệ thống tự động nhận diện User qua Token -> Lấy danh sách từ Giỏ hàng -> Trừ tồn kho -> Tạo Đơn hàng -> Xóa rỗng Giỏ hàng.

## 3. API liên quan
**Module Identity (Đã bảo mật):**
* `POST /api/auth/register`: Đăng ký tài khoản.
* `POST /api/auth/login`: Đăng nhập, trả về chuỗi JWT.

**Module Product:**
* `GET /api/products`: Lấy danh sách (Mở cửa tự do).
* `GET /api/products/{id}`: Xem chi tiết (Mở cửa tự do).
* `POST, PUT, DELETE /api/products/**`: Quản lý sản phẩm (Bắt buộc Token có quyền `ROLE_ADMIN`).
* `POST /api/products/{id}/image`: Upload ảnh sản phẩm (`ROLE_ADMIN`).

**Module Cart (Bảo mật IDOR - Xóa bỏ `userId` trên URL):**
* `GET /api/carts/my-cart`: Xem giỏ hàng của bản thân (Tự lấy ID từ Token).
* `POST /api/carts/add`: Thêm vào giỏ (Body chỉ cần `productId` và `quantity`).
* `DELETE /api/carts/clear`: Xóa trắng giỏ hàng cá nhân.

**Module Order:**
* `POST /api/orders/checkout`: Thanh toán giỏ hàng (Tự lấy ID người đặt từ Token).
* `PUT /api/orders/{id}/status`: Cập nhật trạng thái đơn hàng (Dành cho `ROLE_ADMIN`).

## 4. Các thành phần backend liên quan
* **Cấu trúc thư mục:** Tổ chức theo Feature-based Packaging kết hợp Layered Architecture (`identity/security`, `identity/dto`, `cart/controller`...).
* **Bảo mật (Security):** Tích hợp `Spring Security` và `io.jsonwebtoken`. Chạy chế độ `STATELESS` hoàn toàn.
* **Cấu hình (Config):** `CloudinaryConfig`, `CorsConfig` (Global), `SecurityConfig` (Phân quyền endpoint & chặn CSRF), `ApplicationConfig` (Khai báo PasswordEncoder & UserDetailsService).

## 5. Logic xử lý chính
* **JWT Authentication & Authorization (MỚI):** Request đi vào phải qua `JwtAuthenticationFilter`. Bộ lọc tự động kiểm tra chữ ký (Signature), tính hợp lệ và thời hạn. Nếu chuẩn xác, cấp quyền vào `SecurityContextHolder`.
* **Zero-Trust Frontend (Chống IDOR):** Sử dụng `SecurityUtils.getCurrentUserId()` để tự động móc ID người dùng từ Token. Các API nhạy cảm (Giỏ hàng, Đặt hàng) không còn nhận `userId` từ Client, chặn đứng nguy cơ thao túng dữ liệu chéo.
* **Inter-module Communication:** Các module không gọi chéo Repository của nhau. Liên kết qua Service Layer.
* **DTO Pattern (Data Transfer Object):** Tách biệt hoàn toàn Entity và API Response, ẩn giấu các trường nhạy cảm (như mật khẩu) khi trả về JSON.
* **Dynamic Query (JPQL):** Tìm kiếm, lọc và sắp xếp động trên một method duy nhất.
* **Transaction Management & Order State Machine:** Đóng gói `@Transactional` để rollback dữ liệu nếu có lỗi. Chặn thao tác chuyển trạng thái đơn hàng sai logic, tự động hoàn kho khi hủy đơn.

## 6. Database / Entity liên quan
* **`users`**: Đã implement `UserDetails` của Spring Security. Mật khẩu được mã hóa BCrypt. Thêm cột `role` (Mặc định: `ROLE_USER`).
* **`products`**: Quản lý hàng hóa, tồn kho, link ảnh, cờ trạng thái.
* **`carts` & `cart_items`**: Quản lý giỏ hàng (`orphanRemoval = true`).
* **`orders` & `order_items`**: Quản lý đơn hàng. Lưu Data Snapshot (lưu cứng giá cả vào thời điểm đặt) để bảo toàn lịch sử.

## 7. Đã hoàn thành
- [x] Phân tích và thiết kế Database tối ưu cho phân tán (Microservices-ready).
- [x] Setup môi trường MySQL bằng Docker Compose.
- [x] Xây dựng Module Product (CRUD, Tìm kiếm động, Phân trang, Xóa mềm).
- [x] Tích hợp Cloudinary Upload Ảnh.
- [x] Cấu hình CORS Global & Đồng bộ Encoding UTF-8.
- [x] Áp dụng DTO Pattern toàn dự án.
- [x] **Tích hợp Spring Security & JWT Token (Chạy mô hình Stateless).**
- [x] **Viết `JwtAuthenticationFilter` soi chiếu Token ở cửa ngõ API.**
- [x] **Mã hóa mật khẩu bằng thuật toán BCrypt (`PasswordEncoder`).**
- [x] **Xây dựng `SecurityUtils` và vá triệt để lỗ hổng IDOR cho toàn bộ module Cart và Order.**

## 8. Chưa hoàn thành
- [ ] Xây dựng Trạm xử lý lỗi tập trung (Global Exception Handler) để chuẩn hóa định dạng JSON trả về khi có lỗi (403, 400, 404...).
- [ ] Tách ứng dụng thành Microservices & Tích hợp API Gateway, Service Discovery.
- [ ] Hoàn thiện giao diện Frontend kết nối full API.

## 9. Ghi chú kỹ thuật
* Tuyệt đối không lưu Session trong RAM server (`SessionCreationPolicy.STATELESS`) để sẵn sàng scale up bằng Docker.
* JWT Token được cấu hình để nhét sẵn `userId` vào phần Payload (`extraClaims`), giúp tiết kiệm một nhịp query DB khi Controller cần lấy ID người dùng.
* Lịch sử Git được quản lý theo chuẩn Conventional Commits.

## 10. File đã chỉnh sửa (Snapshot hiện tại)
* **Gốc dự án:** `pom.xml` (Thêm dependency jjwt, spring-security), `docker-compose.yml`, `.env`...
* **Package `identity`:** * Cốt lõi: `User` (implements UserDetails), `UserRepository`, `UserService`.
  * Security: `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `ApplicationConfig`, `SecurityUtils`.
  * API & DTO: `AuthController`, `AuthService`, `LoginRequest`, `RegisterRequest`, `AuthResponse`.
* **Package `product`:** `Product`, `ProductRepository`, `ProductService`, `ProductController`, `CloudinaryConfig`.
* **Package `cart`:** Đã dọn dẹp URL và loại bỏ `userId` khỏi `CartRequest`. Các class: `Cart...`
* **Package `order`:** Đã loại bỏ `userId` khỏi `CheckoutRequest`. Tự động gán user qua JWT. Các class: `Order...`