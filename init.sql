
-- ==================================================
-- 1. MODULE PRODUCT
-- (Quản lý hàng hóa và tồn kho chung 1 bảng)
-- ==================================================
CREATE TABLE products (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(15,2) NOT NULL,
                          stock INT NOT NULL DEFAULT 0,

                          description TEXT,
                          image_url VARCHAR(500),
                          category VARCHAR(100),
                          brand VARCHAR(100),

                          is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==================================================
-- 2. MODULE ORDER
-- (Quản lý việc mua bán)
-- ==================================================
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,              -- ID người mua (giả lập, ko cần bảng User cũng đc)
                        total_price DECIMAL(15, 2) NOT NULL,  -- Tổng tiền đơn hàng
                        status VARCHAR(20) DEFAULT 'CREATED', -- Trạng thái: CREATED, COMPLETED
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,

    -- Liên kết lỏng với Product (Chỉ lưu ID)
                             product_id BIGINT NOT NULL,

                             quantity INT NOT NULL,                -- Mua mấy cái?
                             price DECIMAL(15, 2) NOT NULL,        -- Giá lúc mua là bao nhiêu?

    -- Khóa ngoại nội bộ Module Order (An toàn)
                             FOREIGN KEY (order_id) REFERENCES orders(id)
);