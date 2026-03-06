package com.example.demo.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // API Cho User: Phân trang, Tìm kiếm, Lọc
    // Ví dụ URL: /api/products?page=0&size=10&sortBy=price&keyword=laptop
    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {

        return ResponseEntity.ok(productService.getProducts(page, size, sortBy, keyword, category));
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
}