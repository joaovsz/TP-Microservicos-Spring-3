package com.faculdade.order.controller;

import com.faculdade.order.client.AuthClient;
import com.faculdade.order.dto.OrderRequest;
import com.faculdade.order.dto.OrderResponse;
import com.faculdade.order.model.Order;
import com.faculdade.order.repository.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final AuthClient authClient;

    public OrderController(OrderRepository orderRepository, AuthClient authClient) {
        this.orderRepository = orderRepository;
        this.authClient = authClient;
    }

    @GetMapping
    public Flux<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(order -> authClient.getUserSummary(order.getCustomer())
                        .map(user -> OrderResponse.of(order, user.role()))
                        .defaultIfEmpty(OrderResponse.of(order, "ROLE_USER"))
                );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderResponse>> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .flatMap(order -> authClient.getUserSummary(order.getCustomer())
                        .map(user -> OrderResponse.of(order, user.role()))
                        .defaultIfEmpty(OrderResponse.of(order, "ROLE_USER"))
                )
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request,
                                                           Authentication authentication) {
        String customer = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "authenticated-user";

        Order order = new Order(
                customer,
                request.getItem(),
                request.getQuantity(),
                request.getTotalPrice(),
                "CREATED"
        );

        return orderRepository.save(order)
                .flatMap(savedOrder -> authClient.getUserSummary(savedOrder.getCustomer())
                        .map(user -> OrderResponse.of(savedOrder, user.role()))
                        .defaultIfEmpty(OrderResponse.of(savedOrder, "ROLE_USER"))
                )
                .map(response -> ResponseEntity
                        .created(URI.create("/api/orders/" + response.id()))
                        .body(response)
                );
    }
}
