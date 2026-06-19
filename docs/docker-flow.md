# Docker (Containerization)

---

## 1. Network Isolation
Hệ thống đặt trong custom bridge network mang `techstore-net`.

* **Internal Network:** Các dịch vụ lõi (`user-service`, `order-service`, `db`, `redis`, `rabbitmq`).
* **Single Entrypoint:** Chỉ `api-gateway` được expose port `8080` ra ngoài để nhận request từ Frontend (React) và điều hướng vào bên trong.

---

## 2. Startup Order & Healthcheck

1. **Infrastructure:** MySQL, Redis, và RabbitMQ được khởi động đầu tiên. Lệnh `healthcheck` sẽ liên tục ping (ví dụ: `mysqladmin ping`) cho đến khi dịch vụ sẵn sàng nhận kết nối.
2. **Discovery:** `eureka-server` khởi động và đợi cho đến khi endpoint `/actuator/health` trả về status 200.
3. **Microservices:** Các service (User, Product, Order...) khởi chạy khi `condition: service_healthy` của cả DB, Cache, Message Broker và Eureka đã thỏa mãn.

---

## 3. Multi-stage Build

* **Stage 1 (Builder):** Sử dụng `maven:3.9.6-eclipse-temurin-21-alpine`. `go-offline` để cache dependency, sau đó build mã nguồn thành file `.jar`.
* **Stage 2 (Runtime):** Sử dụng image `eclipse-temurin:21-jre-alpine`. Copy duy nhất file `.jar` từ Stage 1 sang.
* **Timezone:** Cấu hình  `Asia/Ho_Chi_Minh` (TZ) để đảm bảo đồng bộ thời gian cho các giao dịch VNPay và lưu vết Database.

---

## 4. Volumes & Environment
* **Persistent Data:** Sử dụng Docker Named Volumes (`techstore_db_data`, `techstore_redis_data`, `techstore_rabbitmq_data`) để mount dữ liệu ra ngoài.
* **Database Seeding:** Mount file `./init.sql` vào `docker-entrypoint-initdb.d/` để tự động tạo cấu trúc bảng và dữ liệu mẫu ngay lần khởi chạy đầu tiên.
* **Environment Variables:** Toàn bộ thông tin nhạy cảm (Mật khẩu DB, JWT Secret, VNPay Keys) được inject từ file `.env` bên ngoài.

---

## 5. Launch & Build
Tại thư mục root (nơi chứa file `docker-compose.yml`):

```bash
# Đảm bảo đã cấu hình xong file .env
docker-compose up -d --build