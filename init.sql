
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

INSERT INTO products
(name, price, stock, description, image_url, category, brand, is_active)
VALUES
    -- LAPTOP (10)
    (
        'ASUS ROG Strix G16',
        35990000,
        5,
        'Laptop gaming RTX 4060, Intel Core i7-13650HX, RAM 16GB, SSD 512GB',
        'https://res.cloudinary.com/demo/image/upload/rogstrixg16.jpg',
        'LAPTOP',
        'ASUS',
        TRUE
    ),
    (
        'ASUS TUF Gaming A15',
        26990000,
        7,
        'Laptop gaming Ryzen 7, RTX 4050, màn hình 144Hz',
        'https://res.cloudinary.com/demo/image/upload/tufgaminga15.jpg',
        'LAPTOP',
        'ASUS',
        TRUE
    ),
    (
        'ASUS Vivobook 15',
        15990000,
        12,
        'Laptop học tập văn phòng Intel Core i5, RAM 16GB, SSD 512GB',
        'https://res.cloudinary.com/demo/image/upload/vivobook15.jpg',
        'LAPTOP',
        'ASUS',
        TRUE
    ),
    (
        'Dell Inspiron 15 3530',
        17990000,
        10,
        'Laptop văn phòng màn hình 15.6 inch, Intel Core i5, SSD 512GB',
        'https://res.cloudinary.com/demo/image/upload/dellinspiron15.jpg',
        'LAPTOP',
        'DELL',
        TRUE
    ),
    (
        'Dell XPS 13',
        32990000,
        4,
        'Laptop cao cấp thiết kế mỏng nhẹ, màn hình sắc nét, Intel Core i7',
        'https://res.cloudinary.com/demo/image/upload/dellxps13.jpg',
        'LAPTOP',
        'DELL',
        TRUE
    ),
    (
        'Dell G15 Gaming',
        28990000,
        6,
        'Laptop gaming hiệu năng cao, RTX 4050, tản nhiệt tốt',
        'https://res.cloudinary.com/demo/image/upload/dellg15.jpg',
        'LAPTOP',
        'DELL',
        TRUE
    ),
    (
        'MacBook Air M2',
        28990000,
        8,
        'MacBook Air chip Apple M2, màn hình Liquid Retina, pin lâu',
        'https://res.cloudinary.com/demo/image/upload/macbookairm2.jpg',
        'LAPTOP',
        'APPLE',
        TRUE
    ),
    (
        'MacBook Pro 14 M3',
        45990000,
        3,
        'MacBook Pro hiệu năng mạnh với chip Apple M3, phù hợp lập trình và thiết kế',
        'https://res.cloudinary.com/demo/image/upload/macbookpro14m3.jpg',
        'LAPTOP',
        'APPLE',
        TRUE
    ),
    (
        'LG Gram 16',
        31990000,
        5,
        'Laptop siêu nhẹ màn hình lớn 16 inch, pin bền, phù hợp di chuyển',
        'https://res.cloudinary.com/demo/image/upload/lggram16.jpg',
        'LAPTOP',
        'LG',
        TRUE
    ),
    (
        'Lenovo Legion 5',
        30990000,
        6,
        'Laptop gaming RTX 4060, màn hình 165Hz, hiệu năng ổn định',
        'https://res.cloudinary.com/demo/image/upload/legion5.jpg',
        'LAPTOP',
        'LENOVO',
        TRUE
    ),

    -- MAN_HINH (10)
    (
        'ASUS TUF Gaming VG27AQ',
        7990000,
        9,
        'Màn hình gaming 27 inch, 2K, IPS, tần số quét 165Hz',
        'https://res.cloudinary.com/demo/image/upload/vg27aq.jpg',
        'MAN_HINH',
        'ASUS',
        TRUE
    ),
    (
        'ASUS ProArt Display PA278QV',
        8990000,
        5,
        'Màn hình đồ họa 27 inch, 2K, chuẩn màu tốt cho thiết kế',
        'https://res.cloudinary.com/demo/image/upload/pa278qv.jpg',
        'MAN_HINH',
        'ASUS',
        TRUE
    ),
    (
        'Dell UltraSharp U2723QE',
        12990000,
        4,
        'Màn hình 27 inch 4K, màu sắc chính xác, phù hợp công việc chuyên nghiệp',
        'https://res.cloudinary.com/demo/image/upload/u2723qe.jpg',
        'MAN_HINH',
        'DELL',
        TRUE
    ),
    (
        'Dell SE2422H',
        3290000,
        15,
        'Màn hình văn phòng 24 inch Full HD, thiết kế gọn gàng',
        'https://res.cloudinary.com/demo/image/upload/se2422h.jpg',
        'MAN_HINH',
        'DELL',
        TRUE
    ),
    (
        'LG UltraGear 27GP850',
        9490000,
        6,
        'Màn hình gaming 27 inch, 2K, Nano IPS, 165Hz',
        'https://res.cloudinary.com/demo/image/upload/27gp850.jpg',
        'MAN_HINH',
        'LG',
        TRUE
    ),
    (
        'LG 24MP400',
        2890000,
        14,
        'Màn hình 24 inch Full HD, IPS, phù hợp học tập và văn phòng',
        'https://res.cloudinary.com/demo/image/upload/24mp400.jpg',
        'MAN_HINH',
        'LG',
        TRUE
    ),
    (
        'Samsung Odyssey G5',
        6990000,
        8,
        'Màn hình cong 27 inch, 2K, 144Hz, phù hợp chơi game',
        'https://res.cloudinary.com/demo/image/upload/odysseyg5.jpg',
        'MAN_HINH',
        'SAMSUNG',
        TRUE
    ),
    (
        'Samsung Smart Monitor M7',
        8490000,
        5,
        'Màn hình 32 inch 4K tích hợp tính năng giải trí thông minh',
        'https://res.cloudinary.com/demo/image/upload/smartmonitorm7.jpg',
        'MAN_HINH',
        'SAMSUNG',
        TRUE
    ),
    (
        'AOC 24G2',
        4590000,
        11,
        'Màn hình gaming 24 inch IPS, 144Hz, giá tốt',
        'https://res.cloudinary.com/demo/image/upload/aoc24g2.jpg',
        'MAN_HINH',
        'AOC',
        TRUE
    ),
    (
        'ViewSonic VX2728J',
        5690000,
        7,
        'Màn hình gaming 27 inch, Full HD, 180Hz, phản hồi nhanh',
        'https://res.cloudinary.com/demo/image/upload/vx2728j.jpg',
        'MAN_HINH',
        'VIEWSONIC',
        TRUE
    ),

    -- BAN_PHIM (10)
    (
        'ASUS ROG Strix Scope RX',
        2690000,
        10,
        'Bàn phím cơ gaming switch quang học, thiết kế chắc chắn',
        'https://res.cloudinary.com/demo/image/upload/strixscoperx.jpg',
        'BAN_PHIM',
        'ASUS',
        TRUE
    ),
    (
        'ASUS Marshmallow Keyboard KW100',
        890000,
        18,
        'Bàn phím không dây gọn nhẹ, phù hợp học tập và văn phòng',
        'https://res.cloudinary.com/demo/image/upload/kw100.jpg',
        'BAN_PHIM',
        'ASUS',
        TRUE
    ),
    (
        'Keychron K2',
        2190000,
        12,
        'Bàn phím cơ không dây layout nhỏ gọn, hỗ trợ Mac và Windows',
        'https://res.cloudinary.com/demo/image/upload/keychronk2.jpg',
        'BAN_PHIM',
        'KEYCHRON',
        TRUE
    ),
    (
        'Keychron C1',
        1490000,
        14,
        'Bàn phím cơ có dây full size, gõ tốt, phù hợp làm việc',
        'https://res.cloudinary.com/demo/image/upload/keychronc1.jpg',
        'BAN_PHIM',
        'KEYCHRON',
        TRUE
    ),
    (
        'Logitech MX Keys S',
        2790000,
        9,
        'Bàn phím không dây cao cấp, gõ êm, phù hợp dân văn phòng',
        'https://res.cloudinary.com/demo/image/upload/mxkeyss.jpg',
        'BAN_PHIM',
        'LOGITECH',
        TRUE
    ),
    (
        'Logitech K380',
        790000,
        20,
        'Bàn phím Bluetooth nhỏ gọn, kết nối nhiều thiết bị',
        'https://res.cloudinary.com/demo/image/upload/k380.jpg',
        'BAN_PHIM',
        'LOGITECH',
        TRUE
    ),
    (
        'Akko 3068B Plus',
        1890000,
        13,
        'Bàn phím cơ không dây 68 phím, thiết kế đẹp, nhiều chế độ kết nối',
        'https://res.cloudinary.com/demo/image/upload/akko3068bplus.jpg',
        'BAN_PHIM',
        'AKKO',
        TRUE
    ),
    (
        'Akko 3087 DS',
        1590000,
        11,
        'Bàn phím cơ TKL phù hợp chơi game và làm việc',
        'https://res.cloudinary.com/demo/image/upload/akko3087ds.jpg',
        'BAN_PHIM',
        'AKKO',
        TRUE
    ),
    (
        'DareU EK87',
        690000,
        17,
        'Bàn phím cơ TKL giá tốt, phù hợp người mới dùng bàn phím cơ',
        'https://res.cloudinary.com/demo/image/upload/dareuek87.jpg',
        'BAN_PHIM',
        'DAREU',
        TRUE
    ),
    (
        'Razer BlackWidow V4',
        3490000,
        6,
        'Bàn phím cơ gaming cao cấp, switch nhạy, đèn RGB',
        'https://res.cloudinary.com/demo/image/upload/blackwidowv4.jpg',
        'BAN_PHIM',
        'RAZER',
        TRUE
    );