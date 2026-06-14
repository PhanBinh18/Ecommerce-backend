-- ==================================================
-- KHỞI TẠO HẠ TẦNG DATABASE CHO MICROSERVICES
-- ==================================================

-- 1. Tạo 4 database rỗng cho 4 service
CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS cart_db;
CREATE DATABASE IF NOT EXISTS order_db;

-- 2. Cấp quyền truy cập tối đa cho user root để các service dễ dàng kết nối
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;