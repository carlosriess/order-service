package com.tutuman.orderservice.controller;

import com.tutuman.orderservice.client.InventoryClient;
import com.tutuman.orderservice.dto.OrderDto;
import com.tutuman.orderservice.model.Order;
import com.tutuman.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
  private final OrderRepository repository;
  private final InventoryClient inventoryClient;
  private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
  private final StreamBridge streamBridge;
  private final ExecutorService traceableExecutorService;

  @PostMapping
  public String placeOrder(@RequestBody OrderDto dto) {
    circuitBreakerFactory.configureExecutorService(traceableExecutorService);
    Resilience4JCircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory");
    Supplier<Boolean> booleanSupplier =
        () ->
            dto.getOrderLines().stream()
                .allMatch(orderLine -> inventoryClient.checkStock(orderLine.getSkuCode()));
    boolean hasStock = circuitBreaker.run(booleanSupplier, throwable -> handleError());

    if (hasStock) {
      Order order = new Order();
      order.setOrderLines(dto.getOrderLines());
      order.setOrderNumber(UUID.randomUUID().toString());
      repository.save(order);
      log.info("Order placed successfully");
      streamBridge.send(
          "notificationEventSupplier-out-0", MessageBuilder.withPayload(order.getId()));
      return "Order placed successfully";
    } else return "Order failed .Please try again when there is stock available.";
  }

  private boolean handleError() {
    return false;
  }
}
