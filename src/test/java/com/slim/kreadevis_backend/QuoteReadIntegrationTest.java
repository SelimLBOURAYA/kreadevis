package com.slim.kreadevis_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slim.kreadevis_backend.entity.*;
import com.slim.kreadevis_backend.repository.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Non-transactional integration test — catches LazyInitializationException regressions
 * that slice tests (@WebMvcTest, @DataJpaTest) can't surface.
 *
 * No @Transactional on the class, and open-in-view is false — the session closes
 * between repository writes and HTTP reads, so any missing fetch join or missing
 * @Transactional(readOnly=true) on a service method will fail here.
 *
 * Note: Liquibase is NOT exercised here (Spring Boot 4 removed built-in Liquibase
 * auto-configuration; the project needs a dedicated lot to restore it,
 * see lot 10 audit note). The schema is managed via ddl-auto: create-drop on H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class QuoteReadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteItemRepository quoteItemRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String jwtToken;
    private Long quoteId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Create role + user
        Role role = new Role();
        role.setName(ERole.ROLE_USER);
        roleRepository.saveAndFlush(role);

        User user = new User();
        user.setLogin("integration-user");
        user.setEmail("integration@test.com");
        user.setPassword(passwordEncoder.encode("password12"));
        user.setAuthorities(Set.of(role));
        userRepository.saveAndFlush(user);

        // 2. Login to get JWT token
        String loginBody = """
                {"email":"integration@test.com","password":"password12"}""";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertThat(jwtToken).isNotBlank();

        // 3. Create address + client
        Address address = new Address();
        address.setStreet("1 Rue du Test");
        address.setCity("Paris");
        address.setPostalCode("75001");
        addressRepository.saveAndFlush(address);

        Client client = new Client();
        client.setLastName("Dupont");
        client.setEmail("dupont@test.com");
        client.setAddress(address);
        client.setCreatedBy(user);
        clientRepository.saveAndFlush(client);

        // 4. Create product
        Product product = Product.builder()
                .label("Test Product")
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("20"))
                .stockQuantity(10L)
                .referenceCode("TEST-001")
                .createdBy(user)
                .build();
        productRepository.saveAndFlush(product);

        // 5. Create quote with items
        Quote quote = new Quote();
        quote.setClient(client);
        quote.setCreatedBy(user);
        quote.setStatus(QuoteStatus.DRAFT);
        quoteRepository.saveAndFlush(quote);

        QuoteItem item = QuoteItem.builder()
                .quote(quote)
                .product(product)
                .quantity(2L)
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("20"))
                .totalPrice(new BigDecimal("200.00"))
                .build();
        quoteItemRepository.saveAndFlush(item);

        quote.setItems(List.of(item));
        quote.recomputeTotals();
        quoteRepository.saveAndFlush(quote);
        quoteId = quote.getId();
    }

    @AfterEach
    void tearDown() {
        quoteItemRepository.deleteAll();
        quoteRepository.deleteAll();
        productRepository.deleteAll();
        clientRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private String auth() {
        return "Bearer " + jwtToken;
    }

    @Test
    void getQuoteById_shouldReturn200_withItems() throws Exception {
        String responseBody = mockMvc.perform(get("/api/quotes/{id}", quoteId)
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quoteId))
                .andExpect(jsonPath("$.totalPriceHt").isNumber())
                .andExpect(jsonPath("$.totalVat").isNumber())
                .andExpect(jsonPath("$.totalPriceTtc").isNumber())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].totalPrice").value(200.00))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(responseBody);
        assertThat(root.get("items").size()).isEqualTo(1);
    }

    @Test
    void getQuotePdf_shouldReturn200_withApplicationPdf() throws Exception {
        mockMvc.perform(get("/api/quotes/{id}/pdf", quoteId)
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));
    }
}
