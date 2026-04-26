# (Network Map)

| Dịch vụ | Cổng (Port) | URL truy cập nội bộ | Chức năng chính |
| :--- | :--- | :--- | :--- |
| **Eureka Server** | `8761` | `http://localhost:8761` | Service Registry (Sổ danh bạ mạng nội bộ) |
| **API Gateway** | `8080` | `http://localhost:8080` | Cổng duy nhất nhận request từ Frontend (React) |
| **Identity Service** | `8081` | `http://localhost:8081` | Quản lý User, Roles, Xác thực (Auth) |
| **Product Service** | `8082` | `http://localhost:8082` | Quản lý sản phẩm, danh mục, kho hàng |
| **Cart Service** | `8083` | `http://localhost:8083` | Quản lý giỏ hàng của user |
| **Order Service** | `8084` | `http://localhost:8084` | Quản lý đơn hàng, thanh toán |
| **MySQL Server** | `3306` | `localhost:3306` | Chứa các database schema riêng biệt cho từng service |

> **Lưu ý:** Tất cả các service đều sử dụng **Spring Boot 4.0.2** và **Java 23**.