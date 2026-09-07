package com.faculdade.order;

import com.faculdade.order.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static MockWebServer mockAuthServer;

    @BeforeAll
    static void setUpAuthMockServer() throws IOException {
        mockAuthServer = new MockWebServer();
        // Dispatcher dinâmico para responder com base no username requisitado
        mockAuthServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.startsWith("/auth/users/")) {
                    String username = path.substring("/auth/users/".length());
                    String role = username.contains("admin") ? "ROLE_ADMIN" : "ROLE_USER";
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(String.format("{\"id\":10,\"username\":\"%s\",\"role\":\"%s\"}", username, role));
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        mockAuthServer.start();
    }

    @AfterAll
    static void tearDownAuthMockServer() throws IOException {
        if (mockAuthServer != null) {
            mockAuthServer.shutdown();
        }
    }

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

        registry.add("auth.service.url", () -> String.format("http://localhost:%s", mockAuthServer.getPort()));
    }

    @Autowired
    private WebTestClient webTestClient;

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
    void testPublicEndpointAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/api/public/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ONLINE")
                .jsonPath("$.service").isEqualTo("order-service");
    }

    @Test
    @DisplayName("Endpoint protegido /api/orders sem token deve retornar 401 Unauthorized")
    void testProtectedOrdersWithoutTokenFails() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Endpoint protegido com token forjado/inválido deve retornar 401 Unauthorized")
    void testProtectedOrdersWithInvalidTokenFails() {
        webTestClient.get()
                .uri("/api/orders")
                .header("Authorization", "Bearer token-completamente-invalido")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("Endpoint protegido usando refresh token deve ser rejeitado com 401 (somente access token é aceito)")
    void testProtectedOrdersWithRefreshTokenFails() {
        String refreshToken = createTestToken("admin", "ROLE_ADMIN", "REFRESH", 60000);

        webTestClient.get()
                .uri("/api/orders")
                .header("Authorization", "Bearer " + refreshToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("Endpoint protegido com access token válido deve retornar 200 OK e pedidos enriquecidos")
    void testProtectedOrdersWithValidAccessTokenSucceeds() {
        String accessToken = createTestToken("admin", "ROLE_ADMIN", "ACCESS", 60000);

        webTestClient.get()
                .uri("/api/orders")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].item").isNotEmpty()
                .jsonPath("$[0].customerRole").isNotEmpty();
    }

    @Test
    @DisplayName("Criação de pedido com access token válido deve retornar 201 Created e enriquecer via AuthClient")
    void testCreateOrderWithValidTokenSucceeds() {
        String accessToken = createTestToken("engenheiro_teste", "ROLE_USER", "ACCESS", 60000);
        OrderRequest request = new OrderRequest("Kit Hélices Tripá Hartzell", 2, 8500.00);

        webTestClient.post()
                .uri("/api/orders")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.item").isEqualTo("Kit Hélices Tripá Hartzell")
                .jsonPath("$.customer").isEqualTo("engenheiro_teste")
                .jsonPath("$.customerRole").isEqualTo("ROLE_USER")
                .jsonPath("$.totalPrice").isEqualTo(8500.00)
                .jsonPath("$.status").isEqualTo("CREATED");
    }

    @Test
    @DisplayName("Buscar pedido existente por ID deve retornar 200 OK com o pedido enriquecido")
    void testGetOrderByIdSucceeds() {
        String accessToken = createTestToken("admin", "ROLE_ADMIN", "ACCESS", 60000);

        webTestClient.get()
                .uri("/api/orders/1")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.customer").isEqualTo("admin")
                .jsonPath("$.customerRole").isEqualTo("ROLE_ADMIN");
    }
}
