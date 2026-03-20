package com.example.demo.order;

import com.example.demo.cart.Cart;
import com.example.demo.cart.CartItem;
import com.example.demo.cart.CartService;
import com.example.demo.product.Product;
import com.example.demo.product.ProductService;
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
    public Order checkout(CheckoutRequest request) {
        // 1. Lấy giỏ hàng của user
        Cart cart = cartService.getCartByUserId(request.getUserId());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 2. Khởi tạo Đơn hàng
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING"); // Đơn hàng mới luôn ở trạng thái Chờ xử lý

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 3. Duyệt qua từng món trong Giỏ hàng
        for (CartItem cartItem : cart.getItems()) {
            // productService.reduceStock() sẽ tự động check kho, trừ kho và trả về thông tin SP mới nhất
            Product product = productService.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

            // Tạo chi tiết đơn hàng (Lưu lại Snapshot giá cả)
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice()); // Lưu giá trị thực tại thời điểm bấm thanh toán
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrder(order);

            orderItems.add(orderItem);

            // Cộng dồn tiền
            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subTotal);
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalAmount);

        // 4. Lưu Đơn hàng vào DB
        Order savedOrder = orderRepository.save(order);

        // 5. CỰC KỲ QUAN TRỌNG: Làm trống giỏ hàng sau khi mua xong
        cartService.clearCart(request.getUserId());

        return savedOrder;
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