package com.camss.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    @Test
    void shouldRefillTokensAfterElapsedTime() throws Exception {
        TokenBucket bucket = new TokenBucket(2, 2);
        long now = System.nanoTime();

        setField(bucket, "tokens", 0.0);
        setField(bucket, "lastRefillTimestampNanos", now - TimeUnit.SECONDS.toNanos(2));

        assertTrue(bucket.tryConsume());
        assertEquals(1.0, getField(bucket, "tokens"));
        assertTrue(bucket.tryConsume());
    }

    @Test
    void shouldNotConsumeWhenRefillIsNotEnoughToReachOneToken() throws Exception {
        TokenBucket bucket = new TokenBucket(2, 1);
        long now = System.nanoTime();

        setField(bucket, "tokens", 0.0);
        setField(bucket, "lastRefillTimestampNanos", now - TimeUnit.MILLISECONDS.toNanos(400));

        assertFalse(bucket.tryConsume());
        assertEquals(0.4, getField(bucket, "tokens"), 0.05);
    }

    @Test
    void shouldRespectCapacityUnderSimpleConcurrentAccess() throws Exception {
        TokenBucket bucket = new TokenBucket(1, 0);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(bucket::tryConsume);
            }

            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(task));
            }

            int allowed = 0;
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        allowed++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } catch (ExecutionException e) {
                    throw new AssertionError("La ejecución concurrente falló", e.getCause());
                }
            }

            assertEquals(1, allowed);
            assertEquals(0.0, getField(bucket, "tokens"));
        } finally {
            executor.shutdownNow();
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static double getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getDouble(target);
    }
}
