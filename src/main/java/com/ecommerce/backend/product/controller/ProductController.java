package com.ecommerce.backend.product.controller;

import com.ecommerce.backend.product.service.ProductService;
import com.ecommerce.backend.product.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // API Cho User: Phân trang, Tìm kiếm, Lọc
    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size, // Cập nhật size thành 12 cho đẹp với grid UI
            @RequestParam(required = false) String sortType, // Đổi tên biến để khớp với logic mới
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand) {  // Bổ sung tham số brand

        return ResponseEntity.ok(productService.getProducts(page, size, sortType, keyword, category, brand));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // API Cho Admin: Tạo mới
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    // API Cho Admin: Cập nhật
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    // API Cho Admin: Xóa mềm
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok("Đã ẩn sản phẩm thành công!");
    }

    // API Cho Admin: Thống kê sắp hết hàng
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    // API Cho Admin: Upload ảnh sản phẩm
    @PostMapping("/{id}/image")
    public ResponseEntity<String> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = productService.uploadImage(id, file);
            return ResponseEntity.ok("Upload thành công! Link ảnh: " + imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Lỗi khi upload ảnh: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}