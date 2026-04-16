package com.worldcup.Back.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final Map<String, CounterWindow> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong(0);

    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${security.rate-limit.requests-per-minute:120}")
    private int requestsPerMinute;

    @Value("${security.rate-limit.strict.requests-per-minute:10}")
    private int strictRequestsPerMinute;

    @Value("${security.rate-limit.strict.path-prefixes:/api/usuarios/me,/api/auth}")
    private String strictPathPrefixes;

    @Value("${security.rate-limit.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Value("${security.rate-limit.cleanup-every:500}")
    private long cleanupEvery;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled || !shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        boolean strictScope = isStrictScope(request);
        int limit = strictScope ? strictRequestsPerMinute : requestsPerMinute;
        String bucketKey = (strictScope ? "STRICT" : "GENERAL") + "|" + clientIp;
        long now = System.currentTimeMillis();

        CounterWindow window = buckets.compute(bucketKey, (ip, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= WINDOW_MS) {
                return new CounterWindow(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (window != null && window.count > Math.max(1, limit)) {
            reject(response, limit);
            return;
        }

        long sequence = requestSequence.incrementAndGet();
        if (cleanupEvery > 0 && sequence % cleanupEvery == 0) {
            cleanupStaleBuckets(now);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/");
    }

    private boolean isStrictScope(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return false;
        }

        return Arrays.stream(String.valueOf(strictPathPrefixes).split(","))
                .map(String::trim)
                .filter(prefix -> !prefix.isBlank())
                .anyMatch(path::startsWith);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (!trustProxyHeaders) {
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String first = xForwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }

    private void reject(HttpServletResponse response, int limit) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Has superado el límite de solicitudes por minuto (" + Math.max(1, limit) + "). Inténtalo de nuevo en 60 segundos.\"}");
    }

    private void cleanupStaleBuckets(long now) {
        buckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStartMs) > (WINDOW_MS * 2));
    }

    private static final class CounterWindow {
        private final long windowStartMs;
        private int count;

        private CounterWindow(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
