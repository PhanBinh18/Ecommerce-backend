package com.example.demo.product;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

//    // Lấy danh sách phân trang, tìm kiếm và sắp xếp
//    public Page<Product> getProducts(int page, int size, String sortBy, String keyword, String category) {
//        // Tạo đối tượng phân trang & sắp xếp
//        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
//
//        if (keyword != null && !keyword.isEmpty()) {
//            return productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword, pageable);
//        }
//        if (category != null && !category.isEmpty()) {
//            return productRepository.findByCategoryAndIsActiveTrue(category, pageable);
//        }
//
//        return productRepository.findAllByIsActiveTrue(pageable);
//    }

    // Lấy danh sách phân trang, tìm kiếm và lọc kết hợp
    public Page<Product> getProducts(int page, int size, String sortBy, String keyword, String category) {
        // Tạo đối tượng phân trang & sắp xếp
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        // Gọi thẳng vào hàm JPQL
        return productRepository.searchAndFilterProducts(keyword, category, pageable);
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

    // Hàm quan trọng để Order Service gọi sang (Inter-service communication)
    @Transactional
    public Product reduceStock(Long productId, int quantity) {
        Product product = getProductById(productId); // Đã có check isActive bên trong
        if (product.getStock() < quantity) {
            throw new RuntimeException("Hết hàng: " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }
    // Hàm hoàn kho (Dành cho Order Service gọi khi hủy đơn)
    @Transactional
    public void increaseStock(Long productId, int quantity) {
        // Dùng thẳng Repository để lấy được cả những sản phẩm đã bị ẩn (isActive = false)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm để hoàn kho!"));

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