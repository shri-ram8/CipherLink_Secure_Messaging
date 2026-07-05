package com.cipherlink.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    // Per-IP buckets
    private final Map<String, Bucket> otpBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String ip = getClientIp(request);
        String path = request.getRequestURI();

        // OTP endpoint: max 5 requests per 15 minutes per IP
        if (path.contains("/auth/send-otp") || path.contains("/auth/verify-otp")) {
            Bucket bucket = otpBuckets.computeIfAbsent(ip, k ->
                    Bucket.builder()
                            .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(15)))
                            .build());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Too many requests. Please wait before trying again.\"}");
                return;
            }
        }
        // General API: 200 requests per minute per IP
        else if (path.startsWith("/api/")) {
            Bucket bucket = apiBuckets.computeIfAbsent(ip, k ->
                    Bucket.builder()
                            .addLimit(limit -> limit.capacity(200).refillGreedy(200, Duration.ofMinutes(1)))
                            .build());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Rate limit exceeded.\"}");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri;
        return request.getRemoteAddr();
    }
}
