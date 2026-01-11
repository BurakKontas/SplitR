package tr.kontas.splitr.bus.registry;

import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class SyncRegistry {

    private record Entry(CompletableFuture<BaseResponse> future, long expiresAt) {
            private Entry(CompletableFuture<BaseResponse> future, long expiresAt) {
                this.future = future;
                this.expiresAt = System.currentTimeMillis() + expiresAt;
            }

            boolean isExpired() {
                return System.currentTimeMillis() > expiresAt;
            }
        }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    private final long defaultTimeoutMs;
    private final int maxSize;

    public SyncRegistry() {
        this(60_000L, 10_000);
    }

    public SyncRegistry(long defaultTimeoutMs, int maxSize) {
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.maxSize = maxSize;

        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SyncRegistry-Cleanup");
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                30, 30, TimeUnit.SECONDS
        );
    }

    public CompletableFuture<BaseResponse> register(String id) {
        return register(id, defaultTimeoutMs);
    }

    public CompletableFuture<BaseResponse> register(String id, long timeoutMs) {
        if (map.size() >= maxSize) {
            log.warn("Registry full ({}), cleaning up before register", maxSize);
            cleanupExpiredEntries();

            // Hala dolu mu?
            if (map.size() >= maxSize) {
                CompletableFuture<BaseResponse> rejectedFuture = new CompletableFuture<>();
                rejectedFuture.completeExceptionally(
                        new IllegalStateException("Registry is full, cannot register new request")
                );
                return rejectedFuture;
            }
        }

        CompletableFuture<BaseResponse> future = new CompletableFuture<>();
        Entry entry = new Entry(future, timeoutMs);

        map.put(id, entry);
        log.debug("Registered request {} with timeout {}ms", id, timeoutMs);

        future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    map.remove(id);
                    log.debug("Request {} timed out and removed", id);
                    return null;
                });

        return future;
    }

    public void complete(BaseResponse r) {
        Entry entry = map.remove(r.getId());
        if (entry != null) {
            entry.future.complete(r);
            log.debug("Completed and removed request {}", r.getId());
        } else {
            log.warn("Attempted to complete non-existent or already completed request: {}", r.getId());
        }
    }

    public void completeExceptionally(String id, Throwable ex) {
        Entry entry = map.remove(id);
        if (entry != null) {
            entry.future.completeExceptionally(ex);
            log.debug("Completed exceptionally and removed request {}", id);
        }
    }

    public boolean cancel(String id) {
        Entry entry = map.remove(id);
        if (entry != null) {
            boolean cancelled = entry.future.cancel(true);
            log.debug("Cancelled request {}: {}", id, cancelled);
            return cancelled;
        }
        return false;
    }

    private void cleanupExpiredEntries() {
        int cleaned = 0;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Entry> e : map.entrySet()) {
            Entry entry = e.getValue();

            if (entry.isExpired() || entry.future.isDone()) {
                if (map.remove(e.getKey()) != null) {
                    if (!entry.future.isDone()) {
                        entry.future.completeExceptionally(
                                new TimeoutException("Request expired: " + e.getKey())
                        );
                    }
                    cleaned++;
                }
            }
        }

        if (cleaned > 0) {
            log.info("Cleaned up {} expired/completed entries, current size: {}", cleaned, map.size());
        }
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.forEach((id, entry) -> {
            if (!entry.future.isDone()) {
                entry.future.cancel(true);
            }
        });
        map.clear();
        log.info("Registry cleared");
    }

    public void shutdown() {
        log.info("Shutting down SyncRegistry...");

        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clear();

        log.info("SyncRegistry shutdown complete");
    }

    public boolean isHealthy() {
        return map.size() < maxSize * 0.9;
    }
}