package com.faculdade.auth.service;

import com.faculdade.auth.dto.AuthResponse;
import com.faculdade.auth.dto.LoginRequest;
import com.faculdade.auth.dto.RefreshRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public record UserRecord(String username, String passwordHash, String role) {}

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();

        // Usuários padrão em memória com senhas hasheadas via BCrypt
        users.put("admin", new UserRecord("admin", passwordEncoder.encode("admin123"), "ROLE_ADMIN"));
        users.put("user", new UserRecord("user", passwordEncoder.encode("user123"), "ROLE_USER"));
    }

    public AuthResponse login(LoginRequest request) {
        UserRecord user = users.get(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String accessToken = jwtService.generateAccessToken(user.username(), user.role());
        String refreshToken = jwtService.generateRefreshToken(user.username());

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();

        if (token == null || !jwtService.validateToken(token)) {
            throw new BadCredentialsException("Refresh token inválido ou expirado");
        }

        String tokenType = jwtService.extractTokenType(token);
        if (!"REFRESH".equalsIgnoreCase(tokenType)) {
            throw new BadCredentialsException("Token informado não é um refresh token válido");
        }

        String username = jwtService.extractUsername(token);
        UserRecord user = users.get(username);

        if (user == null) {
            throw new BadCredentialsException("Usuário associado ao token não encontrado");
        }

        String newAccessToken = jwtService.generateAccessToken(user.username(), user.role());
        String newRefreshToken = jwtService.generateRefreshToken(user.username());

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
