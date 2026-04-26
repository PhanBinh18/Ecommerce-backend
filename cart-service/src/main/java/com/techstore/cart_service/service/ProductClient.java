package com.techstore.cart_service.service;

import com.techstore.cart_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tên "product-service" phải khớp với tên đăng ký trên Eureka
@FeignClient(name = "product-service")
public interface ProductClient {

    // Đường dẫn này gọi thẳng sang API lấy chi tiết 1 sản phẩm của bên Product
    @GetMapping("/api/products/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") Long id);
}