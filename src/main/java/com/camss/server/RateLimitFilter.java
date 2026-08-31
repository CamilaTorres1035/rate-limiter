package com.camss.server;

import java.io.IOException;
import java.io.OutputStream;

import com.camss.core.RateLimiterService;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class RateLimitFilter extends Filter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String clientIp = getClientIp(exchange);

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

    private String getClientIp(HttpExchange exchange){
        Headers headers = exchange.getRequestHeaders();

        // Prioridad: Cloudflare (retorna directamente la IP del cliente final)
        String cfIp = headers.getFirst("CF-Connecting-IP");
        if (isValidIp(cfIp)) {
            return cfIp.trim();
        }

        // Encabezado estándar para proxies / Load Balancers (AWS ALB, Nginx, etc.)
        // Formato habitual: "IP_CLIENTE, PROXY_1, PROXY_2"
        String xForwardedFor = headers.getFirst("X-Forwarded-For");
        if (isValidIp(xForwardedFor)) {
            // Se toma la primera IP de la lista, que corresponde al cliente original
            String clientIp = xForwardedFor.split(",")[0].trim();
            if (isValidIp(clientIp)) {
                return clientIp;
            }
        }

        // Encabezado alternativo usado por algunos proxies (ej. Nginx real-ip)
        String xRealIp = headers.getFirst("X-Real-IP");
        if (isValidIp(xRealIp)) {
            return xRealIp.trim();
        }

        // Fallback: Conexión TCP directa (IP del socket)
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim());
    }
}
