package tr.kontas.splitr.consumer.store;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class LruStore implements IdempotencyStore {

    private static class Entry {
        final Object value;
        final long expiresAt;
        final long createdAt;

        Entry(Object value, long ttlMs) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = createdAt + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        long age() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    private final Map<String, Entry> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int maxSize;
    private final long defaultTtlMs;
    private final ScheduledExecutorService cleanupScheduler;
    private long evictionCount = 0;
    private long expiredCount = 0;

    public LruStore() {
        this(500, 60 * 60 * 1000L);
    }

    public LruStore(int maxSize) {
        this(maxSize, 60 * 60 * 1000L);
    }

    public LruStore(int maxSize, long defaultTtlMs) {
        this.maxSize = maxSize;
        this.defaultTtlMs = defaultTtlMs;

        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<String, Entry>(
                        (int) (maxSize * 1.5),
                        0.75f,
                        true
                ) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                        boolean shouldRemove = size() > maxSize;
                        if (shouldRemove) {
                            evictionCount++;
                            log.debug("Evicting oldest entry: {}, age: {}ms",
                                    eldest.getKey(),
                                    eldest.getValue().age());
                        }
                        return shouldRemove;
                    }
                }
        );

        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LruStore-Cleanup");
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                5, 5, TimeUnit.MINUTES
        );
    }

    @Override
    public boolean contains(String id) {
        lock.readLock().lock();
        try {
            Entry entry = cache.get(id);
            if (entry == null) {
                return false;
            }

            if (entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(id);
                    expiredCount++;
                    log.debug("Entry {} expired on access", id);
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
                return false;
            }

            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Object get(String id) {
        lock.readLock().lock();
        try {
            Entry entry = cache.get(id);
            if (entry == null) {
                return null;
            }

            if (entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(id);
                    expiredCount++;
                    log.debug("Entry {} expired on get", id);
                    return null;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            return entry.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(String id, Object value) {
        put(id, value, defaultTtlMs);
    }

    @Override
    public void put(String id, Object value, long ttlMs) {
        long maxTtl = 24 * 60 * 60 * 1000L; // 24h max
        long safeTtl = Math.min(ttlMs, maxTtl);

        lock.writeLock().lock();
        try {
            cache.put(id, new Entry(value, safeTtl));
            log.debug("Stored entry {} with TTL: {}ms, current size: {}", id, safeTtl, cache.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanupExpiredEntries() {
        lock.writeLock().lock();
        try {
            int cleaned = 0;
            Iterator<Map.Entry<String, Entry>> iterator = cache.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Entry> e = iterator.next();
                if (e.getValue().isExpired()) {
                    iterator.remove();
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                expiredCount += cleaned;
                log.info("Cleaned up {} expired entries, current size: {}, total evicted: {}, total expired: {}",
                        cleaned, cache.size(), evictionCount, expiredCount);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int cleanup() {
        int cleaned = 0;
        lock.writeLock().lock();
        try {
            Iterator<Map.Entry<String, Entry>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().isExpired()) {
                    iterator.remove();
                    cleaned++;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return cleaned;
    }

    @Override
    public boolean remove(String id) {
        lock.writeLock().lock();
        try {
            return cache.remove(id) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            evictionCount = 0;
            expiredCount = 0;
            log.info("Store cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isHealthy() {
        return size() < maxSize * 0.9;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down LruStore...");

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
        log.info("LruStore shutdown complete");
    }

    public StoreStats getStats() {
        lock.readLock().lock();
        try {
            return new StoreStats(
                    cache.size(),
                    maxSize,
                    evictionCount,
                    expiredCount,
                    (double) cache.size() / maxSize * 100
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    public record StoreStats(
            int currentSize,
            int maxSize,
            long totalEvictions,
            long totalExpirations,
            double fillPercentage
    ) {
        @Override
        public String toString() {
            return String.format(
                    "LruStore[size=%d/%d (%.1f%%), evictions=%d, expirations=%d]",
                    currentSize, maxSize, fillPercentage, totalEvictions, totalExpirations
            );
        }
    }
}