package com.slim.kreadevis_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.dto.client.ClientRequest;
import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.security.UserDetailsServiceImpl;
import com.slim.kreadevis_backend.service.ClientService;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@Import(ClientControllerTest.TestSecurity.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class ClientControllerTest {

    @TestConfiguration
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
    @MockitoBean ClientService clientService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAll_shouldReturn200WithList_whenAuthenticated() throws Exception {
        when(clientService.findAll()).thenReturn(List.of(dummyResponse()));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].lastName").value("Dupont"));
    }

    @Test
    @WithMockUser
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(clientService.findById(99L)).thenThrow(new EntityNotFoundException("Client not found: 99"));

        mockMvc.perform(get("/api/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client not found: 99"));
    }

    @Test
    @WithMockUser
    void create_shouldReturn200_whenRequestValid() throws Exception {
        ClientRequest request = new ClientRequest(null, "Dupont", null, null, null, null, null, null, 1L);
        when(clientService.create(any())).thenReturn(dummyResponse());

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void create_shouldReturn400_whenLastNameMissing() throws Exception {
        ClientRequest request = new ClientRequest(null, "", null, null, null, null, null, null, 1L);

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void delete_shouldReturn204() throws Exception {
        doNothing().when(clientService).delete(1L);

        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isNoContent());
    }

    private ClientResponse dummyResponse() {
        return new ClientResponse(1L, null, "Dupont", null, null, null, null);
    }
}
