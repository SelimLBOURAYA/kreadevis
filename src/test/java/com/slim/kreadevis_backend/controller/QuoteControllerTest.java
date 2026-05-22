package com.slim.kreadevis_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.security.JwtUtils;
import com.slim.kreadevis_backend.security.UserDetailsServiceImpl;
import com.slim.kreadevis_backend.service.QuoteItemService;
import com.slim.kreadevis_backend.service.QuoteService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuoteController.class)
@Import(QuoteControllerTest.TestSecurity.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class QuoteControllerTest {

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
    @MockitoBean QuoteService quoteService;
    @MockitoBean QuoteItemService quoteItemService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser
    void getAll_shouldReturn200WithList() throws Exception {
        when(quoteService.findAll(null, null)).thenReturn(List.of(dummyResponse()));

        mockMvc.perform(get("/api/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referenceCode").value("REF-001"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(quoteService.findById(99L)).thenThrow(new EntityNotFoundException("Quote not found: 99"));

        mockMvc.perform(get("/api/quotes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Quote not found: 99"));
    }

    @Test
    @WithMockUser
    void create_shouldReturn200() throws Exception {
        when(quoteService.create(any())).thenReturn(dummyResponse());

        mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuoteRequest(10L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser
    void finalize_shouldReturn200() throws Exception {
        QuoteResponse finalized = new QuoteResponse(1L, "REF-001", LocalDate.now(), BigDecimal.TEN, QuoteStatus.FINALIZED, null, List.of());
        when(quoteService.finalize(1L)).thenReturn(finalized);

        mockMvc.perform(post("/api/quotes/1/finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"));
    }

    @Test
    @WithMockUser
    void cancel_shouldReturn500_whenAlreadyFinalized() throws Exception {
        when(quoteService.cancel(1L)).thenThrow(new IllegalStateException("Cannot cancel a finalized quote"));

        mockMvc.perform(post("/api/quotes/1/cancel"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Cannot cancel a finalized quote"));
    }

    @Test
    @WithMockUser
    void delete_shouldReturn204() throws Exception {
        doNothing().when(quoteService).delete(1L);

        mockMvc.perform(delete("/api/quotes/1"))
                .andExpect(status().isNoContent());
    }

    private QuoteResponse dummyResponse() {
        return new QuoteResponse(1L, "REF-001", LocalDate.now(), BigDecimal.ZERO, QuoteStatus.DRAFT, null, List.of());
    }
}
