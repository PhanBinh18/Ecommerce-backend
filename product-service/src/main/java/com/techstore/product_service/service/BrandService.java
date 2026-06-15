package com.techstore.product_service.service;

import com.techstore.product_service.dto.BrandResponse;
import com.techstore.product_service.entity.Brand;
import java.util.List;

public interface BrandService {
    List<BrandResponse> getActiveBrands();
    Brand getBrandById(Long id);
}