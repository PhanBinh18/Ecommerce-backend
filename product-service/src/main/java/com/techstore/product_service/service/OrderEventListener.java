package com.techstore.product_service.service;

import com.techstore.product_service.config.RabbitMQConfig;
import com.techstore.product_service.event.InventoryResponseEvent;
import com.techstore.product_service.event.OrderCancelledEvent;
import com.techstore.product_service.event.OrderPlacedEvent;
import com.techstore.product_service.event.OrderItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductService productService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Handle ORDER_CONFIRMED events.
     * This listener deducts actual stock and reservedQuantity for each item.
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_CONFIRMED_QUEUE)
    public void handleOrderConfirmed(OrderPlacedEvent event) {
        log.info("📦 [ORDER CONFIRMED] Received Order #{} with {} items", event.getOrderId(),
                (event.getItems() == null ? 0 : event.getItems().size()));
        try {
            List<OrderItemEvent> items = event.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("Order #{} contains no items", event.getOrderId());
            } else {
                // Delegate to ProductService which performs transactional confirm (deduct stock & reserved)
                productService.confirmOrderItems(items);
            }

            // Send success response back to Order service
            InventoryResponseEvent successResponse = new InventoryResponseEvent(event.getOrderId(), true, "Order confirmed: inventory updated");
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "inventory.reply.confirmed", successResponse);

            log.info("✅ Order #{} confirmed processed successfully", event.getOrderId());
        } catch (Exception ex) {
            log.error("❌ Error processing ORDER_CONFIRMED for Order #{}: {}", event.getOrderId(), ex.getMessage(), ex);
            InventoryResponseEvent failResponse = new InventoryResponseEvent(event.getOrderId(), false, "Error confirming order: " + ex.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "inventory.reply.error", failResponse);
        }
    }

    /**
     * Handle ORDER_CANCELLED events.
     * If isTimeout == true: release reservedQuantity only.
     * If isTimeout == false: restore stockQuantity (and reduce reservedQuantity if present).
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("🚫 [ORDER CANCELLED] Received Order #{} (isTimeout={}, reason={}), items={}",
                event.getOrderId(), event.getIsTimeout(), event.getReason(),
                (event.getItems() == null ? 0 : event.getItems().size()));
        try {
            List<OrderItemEvent> items = event.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("Order #{} cancellation contains no items", event.getOrderId());
            } else {
                // Delegate to ProductService for cancel logic (transactional)
                productService.cancelOrderItems(items, event.getIsTimeout());
            }

            // Send success response back to Order service
            InventoryResponseEvent successResponse = new InventoryResponseEvent(event.getOrderId(), true, "Order cancelled: inventory adjusted");
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "inventory.reply.cancelled", successResponse);

            log.info("✅ Order #{} cancellation processed successfully", event.getOrderId());
        } catch (Exception ex) {
            log.error("❌ Error processing ORDER_CANCELLED for Order #{}: {}", event.getOrderId(), ex.getMessage(), ex);
            InventoryResponseEvent failResponse = new InventoryResponseEvent(event.getOrderId(), false, "Error cancelling order: " + ex.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "inventory.reply.error", failResponse);
        }
    }
}