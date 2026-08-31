package com.camss.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    private AtomicLong simulatedTimeNanos;

    @BeforeEach
    void setUp() {
        // Inicializamos el tiempo simulado en 0
        simulatedTimeNanos = new AtomicLong(0);
    }

    private TokenBucket createBucket(double capacity, double refillRatePerSecond) {
        // Inyectamos el reloj simulado controlado por nosotros
        return new TokenBucket(capacity, refillRatePerSecond, simulatedTimeNanos::get);
    }

    @Test
    @DisplayName("Debe permitir consumir tokens hasta agotar la capacidad inicial")
    void testConsumeInitialCapacity() {
        TokenBucket bucket = createBucket(3.0, 1.0);

        assertTrue(bucket.tryConsume(), "Petición 1 permitida");
        assertTrue(bucket.tryConsume(), "Petición 2 permitida");
        assertTrue(bucket.tryConsume(), "Petición 3 permitida");

        assertFalse(bucket.tryConsume(), "Petición 4 rechazada por falta de tokens");
    }

    @Test
    @DisplayName("Debe recargar tokens proporcionalmente al tiempo simulado transcurrido")
    void testTokenRefillOverTime() {
        // Capacidad: 2 tokens. Tasa de recarga: 1 token por segundo (1,000,000,000 ns)
        TokenBucket bucket = createBucket(2.0, 1.0);

        // Agotamos el bucket
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());

        // Simulamos el paso de 1.5 segundos
        long oneAndHalfSecondsInNanos = 1_500_000_000L;
        simulatedTimeNanos.addAndGet(oneAndHalfSecondsInNanos);

        // Ahora debe permitir consumir 1 token acumulado
        assertTrue(bucket.tryConsume(), "Debe permitir consumir el token recargado");
        assertFalse(bucket.tryConsume(), "No debe tener tokens suficientes para una segunda petición");
    }

    @Test
    @DisplayName("La recarga de tokens no debe exceder la capacidad máxima")
    void testRefillDoesNotExceedCapacity() {
        TokenBucket bucket = createBucket(2.0, 1.0);

        // Consumimos 1 token
        assertTrue(bucket.tryConsume());

        // Simulamos el paso de 10 segundos (debería generar 10 tokens, pero la capacidad es 2)
        simulatedTimeNanos.addAndGet(10_000_000_000L);

        // Intentamos consumir 2 tokens (el máximo permitido)
        assertTrue(bucket.tryConsume(), "Consume token 1");
        assertTrue(bucket.tryConsume(), "Consume token 2");

        // El tercer consumo consecutivo debe fallar porque el límite máximo es 2
        assertFalse(bucket.tryConsume(), "Excede la capacidad máxima del bucket");
    }

    @Test
    @DisplayName("Verificación de Concurrencia: Múltiples hilos compitiendo por los mismos tokens")
    void testConcurrentConsumption() throws InterruptedException {
        int threads = 10;
        int capacity = 5;
        
        // Usamos tiempo de sistema real para la prueba de concurrencia
        TokenBucket bucket = new TokenBucket(capacity, 1.0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    if (bucket.tryConsume()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(capacity, successCount.get(), "Exactamente 5 peticiones deben haber sido aceptadas");
        assertEquals(threads - capacity, rejectedCount.get(), "Exactamente 5 peticiones deben haber sido rechazadas");
    }
}