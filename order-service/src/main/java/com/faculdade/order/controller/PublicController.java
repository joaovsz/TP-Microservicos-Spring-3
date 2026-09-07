package com.faculdade.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "order-service");
        response.put("status", "ONLINE");
        response.put("timestamp", Instant.now().toString());
        response.put("description", "API pública de informações do microsserviço de pedidos");
        response.put("authRequirements", "Rotas protegidas em /api/orders/** exigem autenticação via cabeçalho Authorization: Bearer <token>");
        return Mono.just(ResponseEntity.ok(response));
    }
}
