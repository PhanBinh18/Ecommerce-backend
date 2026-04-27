package com.techstore.order_service.client;

import com.techstore.order_service.config.FeignConfig;
import com.techstore.order_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 2. Gọi sang Product Service
@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    // Yêu cầu ProductService phải có API này
    @PutMapping("/api/products/{id}/reduce-stock")
    ProductResponseDTO reduceStock(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity);

    // Yêu cầu ProductService phải có API này
    @PutMapping("/api/products/{id}/increase-stock")
    void increaseStock(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity);
}
