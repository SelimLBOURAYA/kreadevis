package com.slim.kreadevis_backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caps requests to {@code /api/auth/**} at {@code app.rate-limit.auth.*} per IP (5/minute
 * by default) to blunt brute-force and credential-stuffing attempts. In-memory
 * (per-instance) — see security.md §3 for the tradeoffs and the migration path to Redis
 * if the app scales out. Plain {@code @Value} (rather than a {@code @ConfigurationProperties}
 * bean) so this filter — auto-detected by {@code @WebMvcTest} slices regardless of
 * component scanning — still resolves without a full application context. Test profiles
 * raise the capacity so integration tests sharing a cached Spring context don't trip
 * each other's bucket.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String PROTECTED_PREFIX = "/api/auth/";

    private final int capacity;
    private final int windowSeconds;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.auth.capacity:5}") int capacity,
                           @Value("${app.rate-limit.auth.window-seconds:60}") int windowSeconds) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(PROTECTED_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on {}", ip, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
        }
    }

    private Bucket newBucket() {
        Duration window = Duration.ofSeconds(windowSeconds);
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillIntervally(capacity, window).build())
                .build();
    }
}
