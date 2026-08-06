package com.slim.kreadevis_backend.controller;

import com.slim.kreadevis_backend.dto.user.UserResponse;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.security.UserDetailsServiceImpl;
import com.slim.kreadevis_backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.TestSecurity.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class UserControllerTest {

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
    @MockitoBean UserService userService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getMe_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "jdoe")
    void getMe_shouldReturn200WithUser_whenAuthenticated() throws Exception {
        UserResponse user = new UserResponse(1L, "jdoe", "jdoe@example.com", Set.of("ROLE_USER"));
        when(userService.findByLogin("jdoe")).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("jdoe"))
                .andExpect(jsonPath("$.email").value("jdoe@example.com"))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
    }

    @Test
    @WithMockUser(username = "admin")
    void getMe_shouldReturnAdminRoles() throws Exception {
        UserResponse user = new UserResponse(2L, "admin", "admin@example.com",
                Set.of("ROLE_ADMIN", "ROLE_USER"));
        when(userService.findByLogin("admin")).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
    }

    @Test
    @WithMockUser(username = "ghost")
    void getMe_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findByLogin("ghost"))
                .thenThrow(new EntityNotFoundException("User not found: ghost"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: ghost"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_shouldReturn200_whenAdmin() throws Exception {
        UserResponse user = new UserResponse(1L, "jdoe", "jdoe@example.com", Set.of("ROLE_USER"));
        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("jdoe"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn204_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}
