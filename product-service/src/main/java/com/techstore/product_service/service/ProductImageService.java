package com.techstore.product_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techstore.product_service.entity.Product;
import com.techstore.product_service.entity.ProductImage;
import com.techstore.product_service.repository.ProductImageRepository;
import com.techstore.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final Cloudinary cloudinary;

    /**
     * Upload multiple images to Cloudinary and persist ProductImage entities.
     * Evicts product list/detail caches because images changed.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public List<ProductImage> uploadImages(Long productId, List<MultipartFile> files) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        List<ProductImage> savedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String secureUrl = uploadResult.get("secure_url").toString();

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(secureUrl)
                    .isThumbnail(false)
                    .build();

            ProductImage saved = productImageRepository.save(image);

            // Also add to product.images set (optional)
            product.getImages().add(saved);
            savedImages.add(saved);
        }

        // Persist product to update relationship if needed
        productRepository.save(product);

        return savedImages;
    }

    /**
     * Set a specific image as thumbnail. Clears previous thumbnail.
     * Evicts product caches.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void setThumbnail(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        boolean found = false;
        // ensure images loaded
        Set<ProductImage> images = product.getImages();
        for (ProductImage img : images) {
            if (img.getId() != null && img.getId().equals(imageId)) {
                img.setIsThumbnail(true);
                found = true;
            } else {
                img.setIsThumbnail(false);
            }
            productImageRepository.save(img);
        }

        if (!found) {
            throw new RuntimeException("Image id " + imageId + " does not belong to product " + productId);
        }

        // save product to persist any changes to relationship (optional)
        productRepository.save(product);
    }
}