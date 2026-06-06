package com.trading.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Capacity: 60 requests per bucket
    private static final long BUCKET_CAPACITY = 60;
    // Refill rate: 2 tokens per second (fully refills in 30 seconds)
    private static final double REFILL_RATE_PER_SECOND = 2.0;

    private final ConcurrentHashMap<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip rate limiting for static endpoints or Swagger UI if desired,
        // but applying to all API routes is safer.
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            String clientIp = getClientIp(request);
            TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_RATE_PER_SECOND));

            if (!bucket.tryConsume(1.0)) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"API rate limit exceeded. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class TokenBucket {
        private final long capacity;
        private final double refillRatePerSecond;
        
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }

        public synchronized boolean tryConsume(double amount) {
            refill();
            if (tokens >= amount) {
                tokens -= amount;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedTimeSeconds = (now - lastRefillTime) / 1_000_000_000.0;
            if (elapsedTimeSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedTimeSeconds * refillRatePerSecond);
                lastRefillTime = now;
            }
        }
    }
}
