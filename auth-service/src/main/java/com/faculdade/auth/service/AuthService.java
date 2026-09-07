package com.faculdade.auth.service;

import com.faculdade.auth.dto.AuthResponse;
import com.faculdade.auth.dto.LoginRequest;
import com.faculdade.auth.dto.RefreshRequest;
import com.faculdade.auth.dto.UserSummaryResponse;
import com.faculdade.auth.model.User;
import com.faculdade.auth.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuário associado ao token não encontrado"));

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public UserSummaryResponse getUserSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Usuário '" + username + "' não encontrado"));
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
