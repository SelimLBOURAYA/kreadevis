package com.slim.kreadevis_backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(5, 60);
    }

    @Test
    void doFilter_allowsFirstFiveRequests_thenReturns429_forSameIp() throws Exception {
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = loginRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).as("request #%d", i).isEqualTo(200);
        }

        MockHttpServletRequest sixthRequest = loginRequest();
        MockHttpServletResponse sixthResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(sixthRequest, sixthResponse, filterChain);

        assertThat(sixthResponse.getStatus()).isEqualTo(429);
        assertThat(sixthResponse.getHeader("Retry-After")).isNotNull();
        verify(filterChain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilter_doesNotThrottle_requestsOutsideAuthPrefix() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilter_tracksIpsIndependently() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = loginRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        MockHttpServletRequest otherIpRequest = loginRequest();
        otherIpRequest.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(otherIpRequest, otherIpResponse, filterChain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
