package com.techstore.order_service.service;

import com.techstore.order_service.client.CartClient;
import com.techstore.order_service.client.ProductClient;
import com.techstore.order_service.config.RabbitMQConfig;
import com.techstore.order_service.config.VNPayConfig;
import com.techstore.order_service.dto.*;
import com.techstore.order_service.entity.*;
import com.techstore.order_service.event.*;
import com.techstore.order_service.repository.OrderRepository;
import com.techstore.order_service.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private CartClient cartClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired private VNPayConfig vnPayConfig;

    // -------------------------
    // Checkout
    // -------------------------
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        Long currentUserId = com.techstore.order_service.security.SecurityUtils.getCurrentUserId();
        log.info("User {} starts checkout with paymentMethod={}", currentUserId, request.getPaymentMethod());

        // CHUẨN HÓA 1: Bóc hộp CartResponse thay vì dùng CartDto
        ApiResponse<CartResponse> cartRes = cartClient.getMyCart();
        CartResponse cart = (cartRes != null) ? cartRes.getData() : null;

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Checkout failed: cart empty for user {}", currentUserId);
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // Build reservation requests
        List<ReservationRequest> reservations = cart.getItems().stream()
                .map(item -> ReservationRequest.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // Call reserveStock
        List<ReservationResponse> reservationResponses;
        try {
            ApiResponse<List<ReservationResponse>> apiResponse = productClient.reserveStock(reservations);
            reservationResponses = apiResponse.getData();
            log.info("Received reservation responses: {}", reservationResponses);
        } catch (Exception e) {
            log.error("Error calling ProductService.reserveStock: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gọi Product Service để giữ kho: " + e.getMessage(), e);
        }

        boolean allReserved = reservationResponses.stream().allMatch(ReservationResponse::isReserved);
        if (!allReserved) {
            List<Long> failed = reservationResponses.stream()
                    .filter(r -> !r.isReserved())
                    .map(ReservationResponse::getProductId)
                    .collect(Collectors.toList());
            log.warn("Reservation failed for products: {}", failed);
            throw new RuntimeException("Không thể giữ kho cho các sản phẩm: " + failed);
        }

        // Create order and items snapshot
        Order order = new Order();
        order.setUserId(currentUserId);
        order.setOrderCode("ORD-" + UUID.randomUUID());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setShippingAddress(request.getShippingAddress());

        OrderStatus initialStatus = "VNPAY".equalsIgnoreCase(request.getPaymentMethod()) ?
                OrderStatus.PENDING_UNPAID : OrderStatus.PROCESSING;
        order.setStatus(initialStatus);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemDto cartItem : cart.getItems()) {
            // CHUẨN HÓA 2: Bóc hộp ProductDetailResponse
            ApiResponse<ProductDetailResponse> productApiRes = productClient.getProductById(cartItem.getProductId());
            ProductDetailResponse product = (productApiRes != null) ? productApiRes.getData() : null;

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.getProductId())
                    .productName(product != null ? product.getName() : null)
                    // CHUẨN HÓA 3: Lấy ảnh Thumbnail từ ProductDetailResponse
                    .productImage(product != null ? product.getThumbnailUrl() : null)
                    .price(product != null ? product.getPrice() : BigDecimal.ZERO)
                    .quantity(cartItem.getQuantity())
                    .subTotal((product != null ? product.getPrice() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            total = total.add(item.getSubTotal());
            orderItems.add(item);
        }

        order.setItems(orderItems);
        order.setTotalPrice(total);
        order.setCreatedAt(LocalDateTime.now());

        // Save
        Order saved = orderRepository.save(order);
        log.info("Saved order {} (id={}) for user {}", saved.getOrderCode(), saved.getId(), currentUserId);

        // --- FIX: Reload lại đơn hàng từ DB để đảm bảo lấy đủ danh sách items ---
        Order orderForEvent = orderRepository.findById(saved.getId())
                .orElse(saved);

        // If VNPay, publish expiry event to expiry exchange (TTL queue)
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            OrderExpiryEvent expiryEvent = new OrderExpiryEvent(orderForEvent.getId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXPIRY_EXCHANGE,
                    RabbitMQConfig.ORDER_EXPIRY_ROUTING_KEY,
                    expiryEvent);
            log.info("Published OrderExpiryEvent for orderId={} to exchange={} routingKey={}",
                    orderForEvent.getId(), RabbitMQConfig.ORDER_EXPIRY_EXCHANGE, RabbitMQConfig.ORDER_EXPIRY_ROUTING_KEY);
        } else {
            // For COD/other immediate methods, best-effort clear cart and publish confirmed event
            try {
                cartClient.clearCartInternal(currentUserId);
                log.info("Cleared cart for user {} after COD order {}", currentUserId, orderForEvent.getId());
            } catch (Exception e) {
                log.warn("Failed to clear cart after COD for user {}: {}", currentUserId, e.getMessage());
            }
            List<OrderItemEvent> commitItems = orderForEvent.getItems().stream()
                    .map(i -> new OrderItemEvent(i.getProductId(), i.getQuantity()))
                    .collect(Collectors.toList());
            OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent(orderForEvent.getId(), commitItems);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_CONFIRMED,
                    confirmedEvent);
            log.info("Published OrderConfirmedEvent for orderId={} with {} items",
                    orderForEvent.getId(), commitItems.size());
        }

        String paymentUrl = null;
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            // Gọi hàm sinh URL chuẩn của VNPay (Hàm này ta sẽ viết ở bước 2 bên dưới)
            paymentUrl = createVNPayUrl(saved, vnPayConfig);
            log.info("Generated real VNPAY paymentUrl for order {}: {}", saved.getOrderCode(), paymentUrl);
        }

        return CheckoutResponse.builder()
                .orderId(saved.getId())
                .orderCode(saved.getOrderCode())
                .paymentUrl(paymentUrl)
                .build();
    }

    // -------------------------
    // Confirm order
    // -------------------------
    @Transactional
    public Order confirmOrder(Long orderId) {
        log.info("Confirming order {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id=" + orderId));

        order.setStatus(OrderStatus.PROCESSING);
        Order saved = orderRepository.save(order);
        log.info("Order {} set to PROCESSING", orderId);

        // clear cart best-effort
        try {
            cartClient.clearCartInternal(order.getUserId());
            log.info("Cleared cart after confirming order {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to clear cart after confirming order {}: {}", orderId, e.getMessage());
        }

        // Publish ORDER_CONFIRMED
        List<OrderItemEvent> commitItems = saved.getItems() == null ? Collections.emptyList()
                : saved.getItems().stream()
                .map(i -> new OrderItemEvent(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent(saved.getId(), commitItems);
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CONFIRMED,
                confirmedEvent);
        log.info("Published OrderConfirmedEvent for orderId={} commitItems={}", saved.getId(), commitItems.size());

        return saved;
    }

    // -------------------------
    // Cancel order with reason (Saga Pattern)
    // -------------------------
    @Transactional
    public Order cancelOrder(Long orderId, Long currentUserId, String reason) {
        log.info("User {} is cancelling order {} with reason={}", currentUserId, orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id=" + orderId));

        // 1. Kiểm tra quyền sở hữu đơn hàng
        if (currentUserId != null && !order.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        // 2. Chặn hủy nếu đơn đã đi quá xa
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.SHIPPING) {
            throw new RuntimeException("Đơn hàng đang giao hoặc đã giao, không thể hủy!");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng này đã bị hủy trước đó rồi!");
        }

        // 3. Đổi trạng thái đơn
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order {} set to CANCELLED", orderId);

        // 4. Bắn Event cho Product Service lo việc dọn kho (ĐÃ XÓA Feign Client)
        List<OrderItemEvent> rollbackItems = saved.getItems() == null ? Collections.emptyList()
                : saved.getItems().stream()
                .map(i -> new OrderItemEvent(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        boolean isTimeout = "TIMEOUT".equalsIgnoreCase(reason);
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(saved.getId(), reason, rollbackItems, isTimeout);

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_CANCELLED,
                cancelledEvent);

        log.info("Published OrderCancelledEvent for orderId={} rollbackItems={} isTimeout={}",
                saved.getId(), rollbackItems.size(), isTimeout);

        return saved;
    }

    // -------------------------
    // VNPay IPN handler
    // -------------------------
    @Transactional
    public PaymentTransaction handleVNPayCallback(VNPayIPNRequest ipn) {
        log.info("Received VNPay IPN: vnp_TxnRef={}, vnp_Amount={}, vnp_ResponseCode={}",
                ipn.getVnp_TxnRef(), ipn.getVnp_Amount(), ipn.getVnp_ResponseCode());

        String txnRef = ipn.getVnp_TxnRef();
        if (txnRef == null || txnRef.isEmpty()) {
            log.error("VNPay IPN missing vnp_TxnRef");
            throw new RuntimeException("Missing vnp_TxnRef");
        }

        Order order = orderRepository.findByOrderCode(txnRef)
                .orElseThrow(() -> {
                    log.error("Order not found for orderCode {}", txnRef);
                    return new RuntimeException("Không tìm thấy order với mã: " + txnRef);
                });

        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .transactionCode(UUID.randomUUID().toString())
                .paymentMethod("VNPAY")
                .amount(parseAmount(ipn.getVnp_Amount()))
                .status(ipn.getVnp_ResponseCode())
                .responseCode(ipn.getVnp_ResponseCode())
                .message("VNPay IPN")
                .build();

        PaymentTransaction savedTx = paymentTransactionRepository.save(tx);
        log.info("Saved PaymentTransaction {} for orderCode {}", savedTx.getTransactionCode(), order.getOrderCode());

        if ("00".equals(ipn.getVnp_ResponseCode())) {
            log.info("VNPay payment success for order {}, calling confirmOrder", order.getOrderCode());
            confirmOrder(order.getId());
        } else {
            log.warn("VNPay payment failed for order {}: responseCode={}", order.getOrderCode(), ipn.getVnp_ResponseCode());
            // business decision: keep PENDING_UNPAID or take other actions
        }

        return savedTx;
    }

    private BigDecimal parseAmount(String vnpAmount) {
        if (vnpAmount == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(vnpAmount);
        } catch (Exception e) {
            log.warn("Failed to parse vnpAmount='{}' to BigDecimal", vnpAmount);
            return BigDecimal.ZERO;
        }
    }

    // -------------------------
    // Query methods (Đã cập nhật cho AdminOrderController)
    // -------------------------
    public List<OrderListResponse> getOrderHistory(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        return orders.stream().map(this::toListResponse).collect(Collectors.toList());
    }

    public OrderDetailResponse getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        if (userId != null && !Objects.equals(order.getUserId(), userId)) {
            throw new RuntimeException("Không có quyền truy cập đơn hàng này!");
        }
        return toDetailResponse(order);
    }

    // NÂNG CẤP: Vừa phân trang (Pageable) vừa lọc theo trạng thái (statusStr)
    public OrderPageResponse getAllOrdersForAdmin(Pageable pageable, String statusStr) {
        Page<Order> page;
        if (statusStr != null && !statusStr.isBlank()) {
            OrderStatus status = parseStatus(statusStr);
            page = orderRepository.findByStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }

        // 1. Chuyển đổi Entity sang DTO List
        List<OrderListResponse> contentList = page.map(this::toListResponse).getContent();

        // 2. Gói vào PageResponse tự custom
        return OrderPageResponse.builder()
                .content(contentList)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    // -------------------------
    // Admin update status
    // -------------------------
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        OrderStatus newStatus = parseStatus(newStatusStr);

        if (newStatus == OrderStatus.CANCELLED) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    productClient.increaseStock(item.getProductId(), item.getQuantity());
                    log.info("Increased stock for product {} during admin cancel of order {}", item.getProductId(), orderId);
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Admin updated order {} to status {}", orderId, newStatus);
        return saved;
    }

    private OrderStatus parseStatus(String s) {
        if (s == null) throw new IllegalArgumentException("null status");
        String normalized = s.trim().toUpperCase();
        if ("PENDING".equals(normalized) || "UNPAID".equals(normalized)) return OrderStatus.PENDING_UNPAID;
        if ("PENDING_UNPAID".equals(normalized)) return OrderStatus.PENDING_UNPAID;
        if ("PROCESSING".equals(normalized)) return OrderStatus.PROCESSING;
        if ("SHIPPING".equals(normalized)) return OrderStatus.SHIPPING;
        if ("COMPLETED".equals(normalized) || "DELIVERED".equals(normalized)) return OrderStatus.COMPLETED;
        if ("CANCELLED".equals(normalized) || "CANCELED".equals(normalized)) return OrderStatus.CANCELLED;
        return OrderStatus.valueOf(normalized);
    }

    // -------------------------
    // Mapping helpers
    // -------------------------
    private OrderListResponse toListResponse(Order order) {
        String firstImage = null;
        String firstName = null;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem first = order.getItems().get(0);
            firstImage = first.getProductImage();
            firstName = first.getProductName();
        }
        return OrderListResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .firstProductImage(firstImage)
                .firstProductName(firstName)
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemDto> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                OrderItemDto dto = OrderItemDto.builder()
                        .productId(it.getProductId())
                        .productName(it.getProductName())
                        .productImage(it.getProductImage())
                        .price(it.getPrice())
                        .quantity(it.getQuantity())
                        .subTotal(it.getSubTotal())
                        .build();
                items.add(dto);
            }
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .receiverName(order.getReceiverName())
                .phoneNumber(order.getPhoneNumber())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .note(order.getNote())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }

    // -------------------------
    // Helper: Tạo URL thanh toán VNPay chuẩn
    // -------------------------
    private String createVNPayUrl(Order order, com.techstore.order_service.config.VNPayConfig config) {
        long amount = order.getTotalPrice().longValue() * 100L; // VNPay yêu cầu nhân 100

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", config.getVnp_Version());
        vnp_Params.put("vnp_Command", config.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", config.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", order.getOrderCode());
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", config.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", "127.0.0.1"); // Tạm để localhost, nếu có HttpServletRequest thì lấy IP thật

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Thời hạn thanh toán 15 phút
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp các tham số theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error encoding VNPay params: {}", e.getMessage());
        }

        // Tạo chữ ký (Secure Hash)
        String queryUrl = query.toString();
        String vnp_SecureHash = config.hmacSHA512(config.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return config.getVnp_PayUrl() + "?" + queryUrl;
    }
}