# 📝 Phân tích Luồng dữ liệu (Flow) và @Transactional: Module Order

## 1. Luồng chạy tổng thể của một Request Thanh toán (Checkout Data Flow)

Khi người dùng (đã đăng nhập) bấm "Thanh toán" trên Frontend, dữ liệu không gửi lên chi tiết món hàng nữa mà đi qua một luồng khép kín và bảo mật gồm 3 "trạm kiểm soát" cốt lõi:

1. **Trạm 1: Cửa an ninh & Controller (`OrderController`) - Người tiếp nhận**
   - **Kiểm duyệt JWT:** Request đi qua `JwtAuthenticationFilter`. Nếu Token hợp lệ, hệ thống tự động lưu danh tính người dùng vào `SecurityContextHolder`.
   - **Đón nhận Request:** Đón HTTP Request (`POST /api/orders/checkout`).
   - **Trích xuất bảo mật:** Sử dụng `SecurityUtils.getCurrentUserId()` để lấy `userId` trực tiếp từ Token (Loại bỏ hoàn toàn rủi ro IDOR do Frontend thao túng gửi sai ID).
   - **Lấy thông tin giao hàng:** Map chuỗi JSON từ body thành `CheckoutRequest` (chỉ chứa tên người nhận, SĐT, địa chỉ, phương thức thanh toán). Giao `userId` và gói thông tin này cho `OrderService`.

2. **Trạm 2: Service (`OrderService`) - Bộ não nghiệp vụ**
   - **Liên kết Giỏ hàng:** Gọi sang `CartService.getCartByUserId(userId)` để lấy toàn bộ danh sách sản phẩm khách đang muốn mua. Nếu giỏ trống -> Bắn lỗi từ chối tạo đơn.
   - **Khởi tạo Đơn hàng:** Tạo đối tượng `Order` mới, gán `userId`, thông tin giao hàng và thiết lập trạng thái mặc định là `PENDING` (Chờ xử lý).
   - **Vòng lặp xử lý (Duyệt qua từng món trong Giỏ hàng):**
      - *Giao tiếp liên module (Trừ kho):* Gọi `ProductService.reduceStock()` để kiểm tra và trừ ngay số lượng tồn kho thực tế.
      - *Snapshot dữ liệu (Lập hóa đơn):* Tạo `OrderItem`. Chép cứng `productName` và `price` tại đúng thời điểm bấm nút thanh toán. (Bảo vệ dữ liệu lịch sử doanh thu nếu sau này Admin có tăng/giảm giá sản phẩm gốc).
      - *Tính toán:* Nhân giá với số lượng (`subTotal`) và cộng dồn vào `totalPrice` của đơn hàng.
   - **Dọn dẹp:** Gọi `CartService.clearCart(userId)` để xóa sạch giỏ hàng sau khi đã chuyển thành đơn hàng thành công.

3. **Trạm 3: Repository (`OrderRepository`) - Thủ kho DB**
   - Nhận đối tượng `Order` hoàn chỉnh từ Service.
   - Nhờ cơ chế `cascade = CascadeType.ALL`, Spring Data JPA tự động sinh ra các lệnh `INSERT INTO orders...` và `INSERT INTO order_items...`.
   - Lưu đồng loạt toàn bộ cấu trúc cha-con xuống MySQL.

## 2. Giải mã sức mạnh của `@Transactional` trong tạo Đơn hàng

Hàm `checkout` là một giao dịch cực kỳ phức tạp liên quan đến 3 bảng dữ liệu khác nhau (Products, Orders, Carts). Annotation `@Transactional` đóng vai trò bảo vệ hệ thống theo nguyên tắc **All-or-Nothing (Thành công trọn vẹn, hoặc Không có gì cả)**.

### Kịch bản thực tế: Khách đặt 2 món hàng (Laptop và Chuột) nhưng Chuột vừa hết hàng
- Hệ thống trừ kho Laptop thành công (Update bảng `products`).
- Hệ thống quét đến Chuột và phát hiện kho không đủ số lượng -> Ném ra `RuntimeException("Vượt quá tồn kho!")`.
- **Phép màu của `@Transactional` xuất hiện:** Ngay khi Exception bay ra, Spring tự động kích hoạt lệnh **Rollback**. Nó thu hồi lại lệnh trừ kho của Laptop trước đó, hủy bỏ việc tạo Đơn hàng, và giữ nguyên các món hàng trong Giỏ hàng của khách. Hệ thống trở về trạng thái y hệt như trước khi khách bấm nút, đảm bảo không bao giờ có chuyện kho bị trừ oan hay đơn hàng bị tạo lỗi một nửa.

## 3. Quản lý vòng đời đơn hàng (Order State Machine)

Ngoài việc tạo đơn, `OrderService` còn quản lý một "Cỗ máy trạng thái" thông qua hàm `updateOrderStatus` (dành riêng cho Admin), với các quy tắc kiểm soát nghiêm ngặt:

* **Luồng đi chuẩn:** `PENDING` (Chờ xử lý) -> `PROCESSING` (Đang chuẩn bị) -> `SHIPPING` (Đang giao) -> `DELIVERED` (Đã giao thành công).
* **Chặn điểm đóng băng (Terminal States):** Nếu đơn hàng đã ở trạng thái `DELIVERED` hoặc `CANCELLED` (Đã hủy), hệ thống khóa cứng, không cho phép thay đổi trạng thái nữa.
* **Chặn đi lùi:** Đơn hàng đang `SHIPPING` thì không thể lùi về `PENDING`. Hàng đã gửi đi không thể rút lại bằng phần mềm.
* **Tự động Hoàn kho (Inventory Rollback):** Nếu Admin (hoặc khách) chuyển trạng thái đơn hàng sang `CANCELLED`, hệ thống tự động quét qua các `OrderItem` và gọi ngược lại `ProductService.increaseStock()` để cộng trả lại số lượng sản phẩm vào kho, tránh thất thoát tài sản.