package com.techstore.cart_service.repository;

import com.techstore.cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository <CartItem, Long> {
}
