package com.faculdade.order;

import com.faculdade.order.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String createTestToken(String username, String role, String tokenType, long durationMs) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("token_type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + durationMs))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Endpoint público /api/public/info deve retornar 200 sem necessidade de token")
    void testPublicEndpointAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.service").value("order-service"));
    }

    @Test
    @DisplayName("Endpoint protegido /api/orders sem token deve retornar 401 Unauthorized")
    void testProtectedOrdersWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Endpoint protegido com token forjado/inválido deve retornar 401 Unauthorized")
    void testProtectedOrdersWithInvalidTokenFails() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer token-completamente-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Endpoint protegido usando refresh token deve ser rejeitado com 401 (somente access token é aceito)")
    void testProtectedOrdersWithRefreshTokenFails() throws Exception {
        String refreshToken = createTestToken("admin", "ROLE_ADMIN", "REFRESH", 60000);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Endpoint protegido com access token válido deve retornar 200 OK")
    void testProtectedOrdersWithValidAccessTokenSucceeds() throws Exception {
        String accessToken = createTestToken("admin", "ROLE_ADMIN", "ACCESS", 60000);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].item").exists());
    }

    @Test
    @DisplayName("Criação de pedido com access token válido deve retornar 201 Created e vincular o usuário logado")
    void testCreateOrderWithValidTokenSucceeds() throws Exception {
        String accessToken = createTestToken("engenheiro_teste", "ROLE_USER", "ACCESS", 60000);
        OrderRequest request = new OrderRequest("Kit Hélices Tripá Hartzell", 2, 8500.00);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.item").value("Kit Hélices Tripá Hartzell"))
                .andExpect(jsonPath("$.customer").value("engenheiro_teste"))
                .andExpect(jsonPath("$.totalPrice").value(8500.00))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
}
