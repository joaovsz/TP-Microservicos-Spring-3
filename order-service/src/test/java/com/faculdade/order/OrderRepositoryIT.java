package com.faculdade.order;

import com.faculdade.order.model.Order;
import com.faculdade.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName());
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Deve carregar pedidos do seed inicial via R2DBC e verificar com StepVerifier")
    void testFindInitialOrdersWithStepVerifier() {
        Flux<Order> ordersFlux = orderRepository.findAll();

        StepVerifier.create(ordersFlux)
                .expectNextMatches(order -> order.getItem().equals("MacBook Pro M3 Max"))
                .expectNextMatches(order -> order.getItem().equals("Monitor Dell UltraSharp 32 4K"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve salvar e recuperar pedido de forma reativa com StepVerifier")
    void testSaveAndFindOrderReactive() {
        Order newOrder = new Order("usuario_reativo", "Teclado Mecânico Keychron", 1, 950.00, "CREATED");

        Mono<Order> savedOrderMono = orderRepository.save(newOrder)
                .flatMap(saved -> orderRepository.findById(saved.getId()));

        StepVerifier.create(savedOrderMono)
                .assertNext(order -> {
                    assertThat(order.getId()).isNotNull();
                    assertThat(order.getCustomer()).isEqualTo("usuario_reativo");
                    assertThat(order.getItem()).isEqualTo("Teclado Mecânico Keychron");
                    assertThat(order.getTotalPrice()).isEqualTo(950.00);
                    assertThat(order.getStatus()).isEqualTo("CREATED");
                })
                .verifyComplete();
    }
}
