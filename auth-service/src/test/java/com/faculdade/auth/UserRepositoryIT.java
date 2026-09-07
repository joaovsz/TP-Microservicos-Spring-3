package com.faculdade.auth;

import com.faculdade.auth.model.User;
import com.faculdade.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Deve encontrar usuários inseridos pelas migrations do Flyway")
    void testFindSeededUsers() {
        Optional<User> admin = userRepository.findByUsername("admin");
        assertThat(admin).isPresent();
        assertThat(admin.get().getRole()).isEqualTo("ROLE_ADMIN");

        Optional<User> user = userRepository.findByUsername("user");
        assertThat(user).isPresent();
        assertThat(user.get().getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Deve persistir e buscar novo usuário com Spring Data JDBC")
    void testSaveAndFindUser() {
        User newUser = new User("novo_usuario", "hash123", "ROLE_OPERATOR");
        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByUsername("novo_usuario");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("novo_usuario");
        assertThat(found.get().getRole()).isEqualTo("ROLE_OPERATOR");
    }
}
