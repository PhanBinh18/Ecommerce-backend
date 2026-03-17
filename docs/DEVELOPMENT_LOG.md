# NHẬT KÝ PHÁT TRIỂN BACKEND
**Dự án:** Hệ thống E-commerce thiết bị điện tử (Laptop, Màn hình, Bàn phím, Chuột)
**Kiến trúc:** Modular Monolith (Hướng tới Microservices)
**Giai đoạn:** 1 - Xây dựng Core Framework & Nghiệp vụ nền tảng

---

## 1. Mục tiêu chức năng
Xây dựng một hệ thống backend thương mại điện tử chuyên bán đồ điện tử. Mục tiêu cốt lõi là các module phải hoạt động hoàn toàn độc lập (Decoupled), giao tiếp với nhau qua Service Layer thay vì Database (No Foreign Key constraints giữa các bounded contexts), tạo tiền đề vững chắc để dễ dàng phân tách thành Microservices ở Giai đoạn 2.

## 2. User flow / Luồng xử lý
* **Luồng Admin (Quản trị viên):**
    * Thêm mới sản phẩm -> Tải ảnh lên Cloudinary -> Trả về URL an toàn để lưu vào Database.
    * Cập nhật thông tin, giá cả, số lượng tồn kho (stock).
    * Xóa mềm (Soft Delete) các sản phẩm ngừng kinh doanh để không làm hỏng lịch sử đơn hàng.
    * Xem danh sách sản phẩm sắp hết hàng để có kế hoạch nhập kho.
* **Luồng Khách hàng (User):**
    * Đăng ký tài khoản hệ thống (hiện tại lưu plain-text, chuẩn bị tích hợp Security).
    * Xem danh sách sản phẩm (có hỗ trợ phân trang).
    * Tìm kiếm theo tên kết hợp lọc theo danh mục (Category) bằng truy vấn động (Dynamic Query).
    * Thêm sản phẩm vào Giỏ hàng (tự động cộng dồn số lượng nếu sản phẩm đã tồn tại trong giỏ).
    * **Thanh toán (Checkout):** Hệ thống tự động lấy danh sách items từ Giỏ hàng -> Kiểm tra và trừ tồn kho thực tế -> Tạo Đơn hàng -> Xóa rỗng Giỏ hàng.

## 3. API liên quan
**Module Identity:**
* `POST /api/users/register`: Đăng ký tài khoản.
* `GET /api/users/{id}`: Lấy thông tin tài khoản.

**Module Product:**
* `GET /api/products`: Lấy danh sách (Params: `page`, `size`, `sortBy`, `keyword`, `category`).
* `GET /api/products/{id}`: Xem chi tiết 1 sản phẩm (chỉ query các item có `isActive = true`).
* `POST /api/products`: Tạo mới sản phẩm.
* `PUT /api/products/{id}`: Cập nhật thông tin sản phẩm.
* `DELETE /api/products/{id}`: Xóa mềm sản phẩm.
* `POST /api/products/{id}/image`: Upload ảnh sản phẩm (nhận dữ liệu `multipart/form-data`).
* `GET /api/products/low-stock`: Thống kê hàng sắp hết.

**Module Cart:**
* `GET /api/carts/{userId}`: Xem giỏ hàng của user.
* `POST /api/carts/add`: Thêm vào giỏ / Cộng dồn số lượng.
* `DELETE /api/carts/{userId}/clear`: Xóa trắng giỏ hàng sau khi checkout thành công.

**Module Order:**
* `POST /api/orders/checkout`: Thanh toán toàn bộ giỏ hàng và tạo đơn.

## 4. Các thành phần backend liên quan
* **Cấu trúc thư mục:** Tổ chức theo Feature-based Packaging (`identity`, `product`, `cart`, `order`).
* **Cấu hình (Config):** Các cấu hình ngoại vi như `CloudinaryConfig` được đặt tĩnh bên trong module `product` để đảm bảo tính đóng gói (Encapsulation), thuận tiện khi tách service.
* **Quản lý biến môi trường:** Sử dụng file `.env` kết hợp cấu hình `spring.config.import=optional:file:.env` hoặc Plugin IDE để bảo mật API Key của bên thứ 3.
* **Database & ORM:** Sử dụng Spring Data JPA, Hibernate, MySQL chạy trên Docker.

## 5. Logic xử lý chính
* **Inter-module Communication:** Các module không gọi chéo Repository của nhau. Ví dụ: `OrderService` gọi `ProductService.reduceStock()` để xử lý kho.
* **Dynamic Query (JPQL):** Sử dụng kỹ thuật `(:keyword IS NULL OR ...)` để xử lý linh hoạt việc tìm kiếm và lọc dữ liệu trên cùng một method mà không cần viết các câu lệnh `if-else` dài dòng.
* **Cloud Storage Integration:** Tích hợp SDK của Cloudinary để xử lý `MultipartFile`, trả về `secure_url` (HTTPS) thay vì lưu file vật lý trên server, giúp ứng dụng giữ trạng thái Stateless.
* **Transaction Management:** Đóng gói luồng Checkout bằng `@Transactional`. Đảm bảo tính toàn vẹn ACID: Nếu bước tạo Đơn hàng thất bại, việc trừ kho bên Product và dọn giỏ bên Cart sẽ tự động Rollback.

## 6. Database / Entity liên quan
Thiết kế theo triết lý No-Foreign-Key giữa các Bounded Contexts:
* **`users`**: Quản lý thông tin xác thực và địa chỉ.
* **`products`**: Quản lý hàng hóa, tồn kho (`stock`), link ảnh (`imageUrl`), cờ trạng thái (`isActive`).
* **`carts` & `cart_items`**: Quản lý giỏ hàng. Dùng `orphanRemoval = true` để Hibernate tự dọn dẹp item mồ côi khi clear giỏ.
* **`orders` & `order_items`**: Quản lý đơn hàng. Sử dụng kỹ thuật **Data Snapshot** (lưu cứng `price` và `productName` vào `order_items` tại thời điểm tạo đơn) để bảo toàn dữ liệu lịch sử doanh thu nếu giá sản phẩm gốc thay đổi.

## 7. Đã hoàn thành
- [x] Phân tích và thiết kế Database tối ưu cho phân tán (Microservices-ready).
- [x] Setup môi trường MySQL bằng Docker Compose.
- [x] Xây dựng Module Identity cơ bản.
- [x] Xây dựng Module Product (CRUD, Tìm kiếm động, Phân trang, Xóa mềm).
- [x] Tích hợp Cloudinary Upload Ảnh.
- [x] Xây dựng Module Cart (Logic tính toán và cộng dồn item).
- [x] Xây dựng Module Order & Luồng Checkout khép kín.

## 8. Chưa hoàn thành
- [ ] Xây dựng **Order State Machine** (Chuyển đổi trạng thái đơn hàng: PENDING -> PROCESSING -> SHIPPING -> DELIVERED / CANCELLED).
- [ ] Logic Hoàn kho (Rollback Inventory) khi hủy đơn hàng.
- [ ] Bảo mật phân quyền: Tích hợp Spring Security & JWT Token.
- [ ] Tách ứng dụng thành Microservices & Tích hợp API Gateway, Service Discovery.

## 9. Ghi chú kỹ thuật
* Đã xử lý triệt để lỗi đệ quy vô tận khi parse JSON bằng `@JsonIgnore` tại các Entity con (`OrderItem`, `CartItem`).
* Chú ý bảo mật: File `.env` chứa credential của database và Cloudinary phải luôn nằm trong `.gitignore`.
* Lịch sử Git được quản lý theo chuẩn Conventional Commits (ví dụ: `feat(order):...`, `refactor(product):...`).

## 10. File đã chỉnh sửa (Snapshot hiện tại)
* **Gốc dự án:** `pom.xml`, `docker-compose.yml`, `.env`, `application.properties`, `.gitignore`.
* **Package `identity`:** `User`, `UserRepository`, `UserService`, `UserController`.
* **Package `product`:** `Product`, `ProductRepository`, `ProductService`, `ProductController`, `config/CloudinaryConfig`.
* **Package `cart`:** `Cart`, `CartItem`, `CartRequest`, `CartRepository`, `CartService`, `CartController`.
* **Package `order`:** `Order`, `OrderItem`, `CheckoutRequest`, `OrderRepository`, `OrderService`, `OrderController`.