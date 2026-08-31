package com.camss.server;

import java.io.IOException;
import java.io.OutputStream;

import com.camss.core.RateLimiterService;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class RateLimitFilter extends Filter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        boolean allowed = rateLimiterService.allowRequest(clientIp);

        if (allowed) {
            exchange.getResponseHeaders().set("X-RateLimit-Limit", "5");
            chain.doFilter(exchange);
        } else {
            exchange.getResponseHeaders().set("X-RateLimit-Limit", "5");
            exchange.getResponseHeaders().set("X-RateLimit-Remaining", "0");
            exchange.getResponseHeaders().set("Retry-After", "1"); // Tiempo aproximado en segs
            String response = "429 Too Many Requests - Rate limit exceeded";
            exchange.sendResponseHeaders(429, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    @Override
    public String description() {
        return "Filtro de limitación de tasa por IP";
    }

}
