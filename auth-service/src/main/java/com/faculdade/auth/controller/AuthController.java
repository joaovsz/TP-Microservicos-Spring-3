package com.faculdade.auth.controller;

import com.faculdade.auth.dto.AuthResponse;
import com.faculdade.auth.dto.LoginRequest;
import com.faculdade.auth.dto.RefreshRequest;
import com.faculdade.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<com.faculdade.auth.dto.UserSummaryResponse> getUser(@PathVariable String username) {
        return ResponseEntity.ok(authService.getUserSummary(username));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-service",
                "description", "Serviço de autenticação e emissão de tokens JWT operacional"
        ));
    }
}
