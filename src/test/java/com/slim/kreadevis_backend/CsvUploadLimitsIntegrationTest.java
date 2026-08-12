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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack CSV upload hardening checks (security.md §4). A real embedded server is
 * required here (not MockMvc) because {@code spring.servlet.multipart.max-file-size}
 * is enforced by the servlet container's multipart parsing, which MockMvc's mock
 * request does not exercise. Uses the JDK's HttpClient to avoid adding a test-scoped
 * HTTP client dependency for a single test class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class CsvUploadLimitsIntegrationTest {

    @LocalServerPort private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        Role role = new Role();
        role.setName(ERole.ROLE_USER);
        roleRepository.saveAndFlush(role);

        User user = new User();
        user.setLogin("csv-user");
        user.setEmail("csv-user@test.com");
        user.setPassword(passwordEncoder.encode("password123456"));
        user.setAuthorities(Set.of(role));
        userRepository.saveAndFlush(user);

        String loginBody = """
                {"email":"csv-user@test.com","password":"password123456"}""";
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        jwtToken = objectMapper.readTree(loginResponse.body()).get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void importCsv_returns413_whenFileExceedsMaxSize() throws Exception {
        byte[] oversized = new byte[3 * 1024 * 1024]; // 3MB > the 2MB configured limit

        HttpResponse<String> response = uploadCsv(oversized, "text/csv");

        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void importCsv_returns400_whenContentTypeNotAllowed() throws Exception {
        byte[] body = "label,description,stockQuantity,unitPrice,vatRate,referenceCode\n"
                .getBytes(StandardCharsets.UTF_8);

        HttpResponse<String> response = uploadCsv(body, "application/octet-stream");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> uploadCsv(byte[] content, String contentType) throws Exception {
        String boundary = "----kreadevis-test-boundary";
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"products.csv\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] multipartBody = concat(header.getBytes(StandardCharsets.UTF_8), content, footer.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/products/import"))
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] concat(byte[]... chunks) {
        int total = 0;
        for (byte[] chunk : chunks) {
            total += chunk.length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
