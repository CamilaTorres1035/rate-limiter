package com.camss.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimiterService {
    private final double capacity;
    private final double refillRatePerSecond;

    // Almacen de las IPs activas
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Hilo en segundo plano para limpiar memoria
    private final ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor();

    public RateLimiterService(double capacity, double refillRatePerSecond){
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;

        // Limpieza cada 5 minutos de buckets inactivos
        this.janitor.scheduleAtFixedRate(this::cleanInactiveBuckets, 5, 5, TimeUnit.MINUTES);
    }

    public boolean allowRequest(String clientId){
        TokenBucket bucket = buckets.computeIfAbsent(clientId, id -> new TokenBucket(capacity, refillRatePerSecond));
        return bucket.tryConsume();
    }

    private void cleanInactiveBuckets(){
        long now = System.nanoTime();
        long tenMinutesInNanos = TimeUnit.MINUTES.toNanos(10);

        buckets.entrySet().removeIf(entry -> (now-entry.getValue().getLastRefillTimestampNanos()) > tenMinutesInNanos);
    }

    public void shutdown() {
        janitor.shutdown();
    }
}
