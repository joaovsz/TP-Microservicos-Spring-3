package com.faculdade.order.controller;

import com.faculdade.order.dto.OrderRequest;
import com.faculdade.order.model.Order;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final Map<Long, Order> orderRepository = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @PostConstruct
    public void initData() {
        saveOrder(new Order(idSequence.incrementAndGet(), "admin", "MacBook Pro M3 Max", 1, 19999.00, "CONFIRMED"));
        saveOrder(new Order(idSequence.incrementAndGet(), "user", "Monitor Dell UltraSharp 32 4K", 2, 7200.00, "CONFIRMED"));
    }

    private void saveOrder(Order order) {
        orderRepository.put(order.getId(), order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(new ArrayList<>(orderRepository.values()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.get(id);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", "Pedido com ID " + id + " não encontrado"
            ));
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request,
                                            Authentication authentication) {
        String customer = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "authenticated-user";

        Long newId = idSequence.incrementAndGet();
        Order order = new Order(
                newId,
                customer,
                request.getItem(),
                request.getQuantity(),
                request.getTotalPrice(),
                "CREATED"
        );
        orderRepository.put(newId, order);

        URI location = URI.create("/api/orders/" + newId);
        return ResponseEntity.created(location).body(order);
    }
}
