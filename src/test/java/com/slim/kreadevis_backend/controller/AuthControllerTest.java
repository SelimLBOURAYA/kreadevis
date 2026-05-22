package com.slim.kreadevis_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.dto.auth.AuthResponse;
import com.slim.kreadevis_backend.dto.auth.LoginRequest;
import com.slim.kreadevis_backend.dto.auth.RegisterRequest;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.security.UserDetailsServiceImpl;
import com.slim.kreadevis_backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean AuthService authService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void login_shouldReturn200WithToken_whenCredentialsValid() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("fake-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@test.com", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_shouldReturn400_whenFieldsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn200WithToken_whenDataValid() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("new-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("johndoe", "john@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-token"));
    }

    @Test
    void register_shouldReturn400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("johndoe", "not-an-email", "password123"))))
                .andExpect(status().isBadRequest());
    }
}
