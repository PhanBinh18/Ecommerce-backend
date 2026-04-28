package com.techstore.product_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techstore.product_service.entity.Product;
import com.techstore.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private Cloudinary cloudinary; // Inject Cloudinary Bean
    // ==========================================
    // API DÀNH CHO USER (KHÁCH HÀNG)
    // ==========================================

    // Lấy danh sách phân trang, tìm kiếm và lọc kết hợp
    public Page<Product> getProducts(int page, int size, String sortType, String keyword, String category, String brand) {
        // 1. Khởi tạo Sort mặc định (Sản phẩm mới nhất lên đầu)
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        // 2. Dịch yêu cầu sắp xếp từ Frontend
        if (sortType != null && !sortType.isEmpty()) {
            switch (sortType) {
                case "name_asc": // Từ A - Z
                    sort = Sort.by(Sort.Direction.ASC, "name");
                    break;
                case "name_desc": // Từ Z - A
                    sort = Sort.by(Sort.Direction.DESC, "name");
                    break;
                case "price_asc": // Giá thấp đến cao
                    sort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "price_desc": // Giá cao đến thấp
                    sort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                default:
                    // Đề phòng trường hợp frontend gửi sai, vẫn lấy mặc định
                    sort = Sort.by(Sort.Direction.DESC, "id");
                    break;
            }
        }

        // 3. Tạo đối tượng phân trang & sắp xếp
        Pageable pageable = PageRequest.of(page, size, sort);

        // 4. Gọi thẳng vào hàm JPQL với tham số brand mới
        return productRepository.searchAndFilterProducts(keyword, category, brand, pageable);
    }

    // Lấy chi tiết sản phẩm (Dùng cho Cart và Order gọi sang)
    public Product getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        // Khách hàng không được xem sản phẩm đã bị ẩn
        if (!product.getIsActive()) {
            throw new RuntimeException("Sản phẩm này đã ngừng kinh doanh.");
        }
        return product;
    }

    // ==========================================
    // API DÀNH CHO ADMIN
    // ==========================================

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // Cập nhật thông tin sản phẩm
    @Transactional
    public Product updateProduct(Long id, Product details) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        product.setName(details.getName());
        product.setPrice(details.getPrice());
        product.setStock(details.getStock());
        product.setDescription(details.getDescription());
        product.setCategory(details.getCategory());
        product.setBrand(details.getBrand());
        // Giữ nguyên id và isActive

        return productRepository.save(product);
    }

    // Xóa mềm (Soft Delete)
    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        product.setIsActive(false); // Đổi trạng thái thay vì xóa khỏi DB
        productRepository.save(product);
    }

    // Lấy danh sách sắp hết hàng (Ví dụ: dưới 10 cái)
    public List<Product> getLowStockProducts() {
        return productRepository.findByStockLessThanAndIsActiveTrue(10);
    }

    @Transactional
    public Product reduceStock(Long productId, int quantity) {
        // 1. Thực hiện trừ kho trực tiếp dưới DB
        int rowsAffected = productRepository.reduceStockAtomically(productId, quantity);

        // 2. Nếu số dòng bị tác động = 0, nghĩa là ID sai hoặc (quan trọng nhất) là KHÔNG ĐỦ HÀNG
        if (rowsAffected == 0) {
            // Kiểm tra xem do hết hàng hay do không tìm thấy sản phẩm
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

            throw new RuntimeException("Sản phẩm " + product.getName() + " đã hết hàng hoặc không đủ số lượng!");
        }

        // 3. Nếu thành công, trả về thông tin sản phẩm sau khi đã trừ (để Order Service lấy giá/tên)
        return productRepository.findById(productId).get();
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }
    // --- Xử lý Upload ảnh ---
    @Transactional
    public String uploadImage(Long productId, MultipartFile file) throws IOException {
        // 1. Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        // 2. Upload file lên Cloudinary
        // ObjectUtils.emptyMap() nghĩa là ta dùng setting mặc định của Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        // 3. Lấy URL an toàn (HTTPS) từ kết quả trả về
        String imageUrl = uploadResult.get("secure_url").toString();

        // 4. Cập nhật vào Entity và lưu Database
        product.setImageUrl(imageUrl);
        productRepository.save(product);

        return imageUrl;
    }

}