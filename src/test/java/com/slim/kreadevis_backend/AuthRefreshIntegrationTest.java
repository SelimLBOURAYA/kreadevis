package com.slim.kreadevis_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.entity.ERole;
import com.slim.kreadevis_backend.entity.Role;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.repository.RefreshTokenRepository;
import com.slim.kreadevis_backend.repository.RoleRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end refresh-token flow (security.md §5 / lot 16 item 6): rotation on use,
 * and rejection once revoked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class AuthRefreshIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName(ERole.ROLE_USER);
        roleRepository.saveAndFlush(role);

        User user = new User();
        user.setLogin("refresh-user");
        user.setEmail("refresh-user@test.com");
        user.setPassword(passwordEncoder.encode("password123456"));
        user.setAuthorities(Set.of(role));
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void refresh_issuesNewAccessToken_thenRejectsTheSameTokenTwice() throws Exception {
        String loginBody = """
                {"email":"refresh-user@test.com","password":"password123456"}""";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        String refreshBody = objectMapper.writeValueAsString(new RefreshRequestBody(refreshToken));
        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Note: the new access token can be byte-identical to the old one if issued within
        // the same second for the same subject — JWTs are deterministic given identical
        // claims. Only the refresh token (random, not derived from claims) is guaranteed
        // to differ.
        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        assertThat(refreshJson.get("accessToken").asText()).isNotBlank();
        assertThat(refreshJson.get("refreshToken").asText()).isNotBlank().isNotEqualTo(refreshToken);

        // Rotation: the first refresh token was single-use and is now revoked.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_forUnknownToken() throws Exception {
        String refreshBody = objectMapper.writeValueAsString(new RefreshRequestBody("not-a-real-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    private record RefreshRequestBody(String refreshToken) {}
}
