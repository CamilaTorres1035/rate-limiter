package com.camss.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException{
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        //TODO: instanciar servicio de RateLimit

        // Crear contexto base y registrar filtro
        HttpContext context = server.createContext("/", new RootHandler());
        context.getFilters().add(null); // TODO: añadir RateLimitFilter

        // Pool de hilos para soportar concurrencia real
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.start();
        System.out.println("Servidor iniciado en http://localhost:" + port);
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/ping") && method.equals("GET")){
                handleGet(exchange);
            } else if(path.equals("/data") && method.equals("POST")){
                handlePost(exchange);
            } else {
                sendResponse(exchange, 404, "404 Not Found");
            }
        }    

        private void handleGet(HttpExchange exchange) throws IOException{
            sendResponse(exchange, 200, "{\"status\": \"pong\"}");
        }
        private void handlePost(HttpExchange exchange) throws IOException{
            sendResponse(exchange, 200, "{\"status\": \"data received\"}");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            byte[] bytes = responseText.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

}
