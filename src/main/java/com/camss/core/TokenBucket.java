package com.camss.core;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {
    private final double capacity;
    private final double refillRatePerNano;

    private double tokens;
    private long lastRefillTimestampNanos;

    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(double capacity, double refillRatePerSecond){
        this.capacity = capacity;
        this.refillRatePerNano = refillRatePerSecond/1_000_000_000.0;
        this.tokens = capacity; // Inicio con bucket lleno
        this.lastRefillTimestampNanos = System.nanoTime();
    }

    public boolean tryConsume(){
        lock.lock();
        try {
            long now = System.nanoTime();
            long elapsed = now - this.lastRefillTimestampNanos;

            // Lazy evaluation
            double tokensToAdd = elapsed*refillRatePerNano;
            this.tokens = Math.min(capacity, this.tokens + tokensToAdd);
            this.lastRefillTimestampNanos = now;

            if (this.tokens >= 1.0){
                this.tokens -= 1.0;
                return true; // petición permitida
            }
            return false; // Rate Limit excedido (429)
        } finally {
            lock.unlock();
        }
    }

    public long getLastRefillTimestampNanos() {
        return this.lastRefillTimestampNanos;
    }
}
