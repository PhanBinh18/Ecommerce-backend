## 1. Dịch vụ Người dùng (User Service)

Đây là "trái tim" của hệ thống phân quyền và định danh, đảm bảo an ninh cho toàn bộ hệ thống E-commerce.

### Quản lý CSDL
`user_db`  
(Gồm các bảng: `Users`, `Roles`, `User_Roles`, `Addresses`)

### Nhiệm vụ chính

#### Xác thực & Phân quyền (Authentication & RBAC)
- Cung cấp API đăng nhập truyền thống.
- Đồng bộ dữ liệu từ Google OAuth2.
- Cấp phát và quản lý vòng đời của JWT Token.

#### Quản lý Khách hàng (Customer Management)
- Cho phép khách hàng xem hồ sơ cá nhân.
- Cập nhật thông tin người dùng.
- CRUD sổ địa chỉ giao hàng.

#### Quản lý Quản trị viên (Admin Management)
- Cung cấp API cho Use Case `Manage Users`.
- Xem danh sách người dùng.
- Kích hoạt / khóa tài khoản vi phạm.
- Phân quyền người dùng.

### Tương tác
- Cung cấp thông tin User/Address cho `Order Service` khi tạo đơn hàng.

---

## 2. Dịch vụ Sản phẩm & Kho (Product Service)

Dịch vụ này kết hợp cả quản lý danh mục sản phẩm (Catalog) và quản lý tồn kho (Inventory) nhằm tránh việc tách thêm một service kho riêng quá nhỏ, gây tăng chi phí vận hành và bảo trì.

### Quản lý CSDL
`product_db`  
(Gồm các bảng: `Categories`, `Products`, `Product_Images`)

### Nhiệm vụ chính

#### Quản lý Danh mục (Category Management)
- Phục vụ Use Case `Manage Categories`.
- Thêm danh mục.
- Chỉnh sửa danh mục.
- Xóa mềm danh mục.

#### Quản lý Sản phẩm (Catalog Management)
- Phục vụ Use Case `Manage Products`.
- Thêm sản phẩm.
- Chỉnh sửa thông tin sản phẩm.
- Xóa mềm sản phẩm.
- Tìm kiếm và phân trang sản phẩm.

#### Quản lý Tồn kho (Inventory Management)
- Xử lý logic trừ kho khi khách đặt hàng.
- Hoàn kho khi đơn hàng bị hủy.

### Tương tác
- Nhận OpenFeign từ `Cart Service` để hiển thị thông tin sản phẩm mới nhất.
- Nhận OpenFeign từ `Order Service` để kiểm tra và trừ kho khi checkout.
- Lắng nghe (Consume) RabbitMQ event từ `Order Service` để thực hiện hoàn kho.

---

## 3. Dịch vụ Đơn hàng (Order Service)

Đây là "bộ não" điều phối các giao dịch mua bán và xử lý giao dịch phân tán theo mô hình Saga Orchestrator.

### Quản lý CSDL
`order_db`  
(Gồm các bảng: `Orders`, `Order_Items`, `Payment_Transactions`)

### Nhiệm vụ chính

#### Tạo và Xử lý Đơn hàng (Checkout Processing)
- Tiếp nhận yêu cầu đặt hàng.
- Tính toán tổng tiền đơn hàng.
- Lưu trữ thông tin hóa đơn.

#### Quản lý Thanh toán (Payment Management)
- Khởi tạo phiên thanh toán VNPay.
- Thiết lập Scheduled Task tự động hủy đơn sau 10 phút nếu chưa thanh toán.

#### Lịch sử và Quản trị Đơn hàng
- Phục vụ Use Case `View Order History` cho khách hàng.
- Phục vụ Use Case `Manage Orders` cho quản trị viên.
- Xem chi tiết đơn hàng.
- Cập nhật trạng thái đơn hàng.

### Tương tác
- Gọi OpenFeign sang `Cart Service` để lấy danh sách sản phẩm trong giỏ hàng.
- Gọi OpenFeign sang `Product Service` để kiểm tra và trừ tồn kho.
- Phát sự kiện (Publish) vào RabbitMQ khi đơn hàng bị hủy hoặc quá hạn thanh toán.

---

## 4. Dịch vụ Giỏ hàng (Cart Service)

Dịch vụ lưu trữ tạm thời với tần suất đọc/ghi cao, nhưng dữ liệu không mang tính ràng buộc nghiệp vụ lâu dài như đơn hàng.

### Quản lý CSDL
`cart_db`  
(Gồm các bảng: `Carts`, `Cart_Items`)

### Nhiệm vụ chính

#### Quản lý Giỏ hàng (Cart Operations)
- Thêm sản phẩm vào giỏ hàng.
- Tăng / giảm số lượng sản phẩm.
- Xóa sản phẩm khỏi giỏ hàng.

#### Dọn dẹp Giỏ hàng
- Xóa toàn bộ giỏ hàng sau khi checkout thành công.

### Tương tác
- Gọi OpenFeign sang `Product Service` để ánh xạ thông tin sản phẩm.
- Nhận OpenFeign từ `Order Service` để xác nhận danh sách sản phẩm khi đặt hàng và xóa giỏ hàng.