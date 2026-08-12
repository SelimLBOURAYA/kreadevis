package com.slim.kreadevis_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.security.UserDetailsServiceImpl;
import com.slim.kreadevis_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(AdminUserControllerTest.TestSecurity.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AdminUserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurity {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }

        @Bean
        MockMvcBuilderCustomizer securityMockMvcCustomizer() {
            return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean UserService userService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void createAdmin_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("newadmin", "newadmin@test.com", "password123456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAdmin_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("newadmin", "newadmin@test.com", "password123456"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAdmin_shouldReturn201WithLocation_whenAdmin() throws Exception {
        UserResponse response = new UserResponse(7L, "newadmin", "newadmin@test.com", Set.of("ROLE_ADMIN"));
        when(userService.createAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("newadmin", "newadmin@test.com", "password123456"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/7"))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("ROLE_ADMIN")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAdmin_shouldReturn400_whenFieldsBlank() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("", "", ""))))
                .andExpect(status().isBadRequest());
    }
}
