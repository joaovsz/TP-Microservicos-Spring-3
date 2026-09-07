package com.faculdade.order.client;

import com.faculdade.order.dto.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthClient {

    private final WebClient webClient;

    public AuthClient(@Value("${auth.service.url:http://localhost:8081}") String authServiceUrl,
                      WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    public Mono<UserSummary> getUserSummary(String username) {
        return webClient.get()
                .uri("/auth/users/{username}", username)
                .retrieve()
                .bodyToMono(UserSummary.class)
                .onErrorResume(e -> Mono.just(new UserSummary(null, username, "ROLE_USER")));
    }
}
