# Cơ chế Bảo mật và Luồng Dữ liệu (Security & Data Flow)

## 1. Cơ chế Bảo mật Tập trung (Centralized Security)
Trong kiến trúc Microservices của TechStore, bảo mật được chia làm 2 lớp:
* **Xác thực (Authentication):** Do `Identity Service` đảm nhận. Phát hành JWT Token khi người dùng đăng nhập thành công.
* **Phân quyền (Authorization):** Do `API Gateway` đảm nhận. Mọi request từ ngoài vào phải đi qua Gateway để kiểm tra tính hợp lệ của Token trước khi đi vào các service bên trong.

## 2. Luồng Xử lý Dữ liệu (Data Flow)

1.  **Đăng nhập:** * Client (React) -> `POST /api/auth/login` -> Gateway (8080) -> Identity Service (8081).
    * Identity Service kiểm tra DB (`techstore_identity`), tạo JWT Token trả về cho Client.
2.  **Truy cập Service Nghiệp vụ (VD: Lấy giỏ hàng):**
    * Client đính kèm JWT Token vào Header: `Authorization: Bearer <token>` và gọi API Gateway.
    * Gateway nhận request, xác minh Token (chữ ký, thời hạn).
    * Nếu hợp lệ, Gateway **giải mã Token** để lấy `userId`, sau đó gắn vào một **HTTP Header nội bộ** tên là `X-User-Id`.
    * Gateway chuyển tiếp (route) request kèm Header `X-User-Id` sang `Cart Service` (8083).
    * `Cart Service` đọc Header `X-User-Id` để lấy thông tin người dùng an toàn mà không cần phải gọi lại `Identity Service`.