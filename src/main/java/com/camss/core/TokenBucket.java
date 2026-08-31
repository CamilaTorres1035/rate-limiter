package com.camss.core;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

public class TokenBucket {
    private final double capacity;
    private final double refillRatePerNano;

    private double tokens;
    private long lastRefillTimestampNanos;

    private final ReentrantLock lock = new ReentrantLock();
    private final LongSupplier nanoClock; // Proveedor de tiempo

    // Constructor de producción
    public TokenBucket(double capacity, double refillRatePerSecond) {
        this(capacity, refillRatePerSecond, System::nanoTime);
    }

    // Constructor para Pruebas Unitarias
    public TokenBucket(double capacity, double refillRatePerSecond, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillRatePerNano = refillRatePerSecond / 1_000_000_000.0;
        this.nanoClock = nanoClock;
        this.tokens = capacity;
        this.lastRefillTimestampNanos = nanoClock.getAsLong();
    }

    public boolean tryConsume() {
        lock.lock();
        try {
            long now = nanoClock.getAsLong(); // Usa el proveedor de tiempo
            long elapsed = now - this.lastRefillTimestampNanos;

            double tokensToAdd = elapsed * refillRatePerNano;
            this.tokens = Math.min(capacity, this.tokens + tokensToAdd);
            this.lastRefillTimestampNanos = now;

            if (this.tokens >= 1.0) {
                this.tokens -= 1.0;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public double getTokens() {
        lock.lock();
        try {
            return this.tokens;
        } finally {
            lock.unlock();
        }
    }

    public long getLastRefillTimestampNanos() {
        return this.lastRefillTimestampNanos;
    }
}