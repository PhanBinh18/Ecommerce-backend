package com.techstore.order_service.service;

import com.techstore.order_service.client.CartClient;
import com.techstore.order_service.client.ProductClient;
import com.techstore.order_service.dto.*;
import com.techstore.order_service.entity.Order;
import com.techstore.order_service.entity.OrderItem;
import com.techstore.order_service.repository.OrderRepository;

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
    private ProductClient productClient; // Lắp điện thoại Product

    @Autowired
    private CartClient cartClient; // Lắp điện thoại Cart

    @Transactional
    public Order checkout(Long userId, CheckoutRequest request) { // <-- 1. Thêm tham số Long userId
        // 1. Gọi điện sang Cart Service lấy giỏ hàng (Không cần truyền userId vì Token đã tự mang theo ID)
        CartDto cart = cartClient.getMyCart();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        Order order = new Order();
        order.setUserId(userId); // <-- 3. Thay request.getUserId() thành userId
        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 2. Duyệt giỏ hàng và gọi điện trừ kho
        for (CartItemDto cartItem : cart.getItems()) {
            try {
                // Gọi API trừ kho, nếu Product sập hoặc hết hàng, nó sẽ văng Exception
                ProductResponseDTO product = productClient.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setPrice(product.getPrice());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setOrder(order);

                orderItems.add(orderItem);

                BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                totalAmount = totalAmount.add(subTotal);
            } catch (Exception e) {
                // Nếu 1 sản phẩm thất bại, phải báo lỗi để dừng toàn bộ đơn hàng
                throw new RuntimeException("Lỗi khi trừ kho sản phẩm ID: " + cartItem.getProductId());
            }
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 3. Gọi điện dọn giỏ hàng
        try {
            cartClient.clearCart();
        } catch (Exception e) {
            System.err.println("Cảnh báo: Tạo đơn thành công nhưng dọn giỏ hàng thất bại!");
        }

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
                try {
                    // SỬA THÀNH productClient
                    productClient.increaseStock(item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi khi kết nối đến Product Service để hoàn kho!");
                }
            }
        }

        // 4. CẬP NHẬT & LƯU
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
