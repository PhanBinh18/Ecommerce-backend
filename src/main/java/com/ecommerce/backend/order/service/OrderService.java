package com.ecommerce.backend.order.service;

import com.ecommerce.backend.cart.dto.CartDto;
import com.ecommerce.backend.cart.dto.CartItemDto;
import com.ecommerce.backend.cart.service.CartService;
import com.ecommerce.backend.order.dto.CheckoutRequest;
import com.ecommerce.backend.order.dto.OrderDto;
import com.ecommerce.backend.order.dto.OrderItemDto;
import com.ecommerce.backend.order.entity.Order;
import com.ecommerce.backend.order.entity.OrderItem;
import com.ecommerce.backend.order.repository.OrderRepository;
import com.ecommerce.backend.product.entity.Product;
import com.ecommerce.backend.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService; // Gọi thêm CartService

    @Transactional
    public Order checkout(Long userId, CheckoutRequest request) { // <-- 1. Thêm tham số Long userId
        // 1. Lấy giỏ hàng của user bằng userId truyền vào
        CartDto cart = cartService.getCartByUserId(userId); // <-- 2. Thay request.getUserId() thành userId

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 2. Khởi tạo Đơn hàng
        Order order = new Order();
        order.setUserId(userId); // <-- 3. Thay request.getUserId() thành userId
        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 3. Duyệt qua từng món trong Giỏ hàng
        for (CartItemDto cartItem : cart.getItems()) {
            Product product = productService.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrder(order);

            orderItems.add(orderItem);

            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subTotal);
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalAmount);

        // 4. Lưu Đơn hàng vào DB
        Order savedOrder = orderRepository.save(order);

        // 5. CỰC KỲ QUAN TRỌNG: Làm trống giỏ hàng sau khi mua xong
        cartService.clearCart(userId); // <-- 4. Thay request.getUserId() thành userId

        return savedOrder;
    }

    // =========================================================
    // HÀM MỚI: Lấy danh sách đơn hàng của User đang đăng nhập
    // =========================================================
    public List<OrderDto> getMyOrders(Long userId) {
        // 1. Lấy danh sách đơn hàng từ DB
        List<Order> orders = orderRepository.findByUserIdOrderByIdDesc(userId);

        // 2. Chuyển đổi từ Entity sang DTO
        List<OrderDto> orderDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderDto dto = new OrderDto();
            dto.setId(order.getId());
            dto.setReceiverName(order.getReceiverName());
            dto.setPhoneNumber(order.getPhoneNumber());
            dto.setShippingAddress(order.getShippingAddress());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setStatus(order.getStatus());
            dto.setTotalPrice(order.getTotalPrice());

            // Map danh sách sản phẩm (Items)
            List<OrderItemDto> itemDtos = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setProductId(item.getProductId());
                itemDto.setProductName(item.getProductName());
                itemDto.setPrice(item.getPrice());
                itemDto.setQuantity(item.getQuantity());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);

            orderDtos.add(dto);
        }

        return orderDtos;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        String currentStatus = order.getStatus();
        newStatus = newStatus.toUpperCase(); // Chuẩn hóa đầu vào

        // 1. CHẶN ĐIỂM ĐÓNG BĂNG (Terminal States)
        if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
            throw new RuntimeException("Đơn hàng đã ở trạng thái " + currentStatus + ", không thể thay đổi nữa!");
        }

        // 2. CHẶN ĐI LÙI (State Machine Validation)
        if (currentStatus.equals("SHIPPING") && (newStatus.equals("PENDING") || newStatus.equals("PROCESSING"))) {
            throw new RuntimeException("Hàng đang giao, không thể lùi trạng thái về " + newStatus);
        }
        if (currentStatus.equals("PROCESSING") && newStatus.equals("PENDING")) {
            throw new RuntimeException("Đơn đã xử lý, không thể lùi về PENDING");
        }

        // 3. LOGIC HOÀN KHO (Chỉ xảy ra khi chuyển sang CANCELLED)
        if (newStatus.equals("CANCELLED")) {
            for (OrderItem item : order.getItems()) {
                // Gọi sang ProductService để cộng trả lại kho
                productService.increaseStock(item.getProductId(), item.getQuantity());
            }
        }

        // 4. CẬP NHẬT & LƯU
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}