package tr.kontas.splitr.bus.store;

import org.junit.jupiter.api.*;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LruStoreTest {

    private LruStore store;

    @BeforeEach
    void setUp() {
        store = new LruStore(5, 500); // max 5 entry, default TTL 500ms
    }

    @AfterEach
    void tearDown() {
        store.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("Should put and get value")
    void testPutAndGet() {
        store.put("id-1", "value-1");

        assertTrue(store.contains("id-1"));
        assertEquals("value-1", store.get("id-1"));
        assertEquals(1, store.size());
    }

    @Test
    @Order(2)
    @DisplayName("Should expire entry after TTL")
    void testExpiration() throws InterruptedException {
        store.put("expire-id", "value", 100);

        Thread.sleep(200);

        assertFalse(store.contains("expire-id"));
        assertNull(store.get("expire-id"));
        assertEquals(0, store.size());
    }

    @Test
    @Order(3)
    @DisplayName("Should evict least recently used entry when max size exceeded")
    void testLruEviction() {
        store.put("id-1", "v1");
        store.put("id-2", "v2");
        store.put("id-3", "v3");
        store.put("id-4", "v4");
        store.put("id-5", "v5");

        // access id-1 so it becomes recently used
        store.get("id-1");

        // this should evict id-2 (LRU)
        store.put("id-6", "v6");

        assertFalse(store.contains("id-2"));
        assertTrue(store.contains("id-1"));
        assertEquals(5, store.size());
    }

    @Test
    @Order(4)
    @DisplayName("Should remove entry explicitly")
    void testRemove() {
        store.put("remove-id", "value");

        boolean removed = store.remove("remove-id");

        assertTrue(removed);
        assertFalse(store.contains("remove-id"));
        assertEquals(0, store.size());
    }

    @Test
    @Order(5)
    @DisplayName("Should cleanup expired entries manually")
    void testManualCleanup() throws InterruptedException {
        store.put("c1", "v1", 100);
        store.put("c2", "v2", 100);

        Thread.sleep(200);

        int cleaned = store.cleanup();

        assertEquals(2, cleaned);
        assertEquals(0, store.size());
    }

    @Test
    @Order(6)
    @DisplayName("Should clear store")
    void testClear() {
        for (int i = 0; i < 5; i++) {
            store.put("id-" + i, "v" + i);
        }

        assertEquals(5, store.size());

        store.clear();

        assertEquals(0, store.size());
    }

    @Test
    @Order(7)
    @DisplayName("Should report healthy and unhealthy states")
    void testHealthCheck() {
        // %90 threshold, max = 5 → healthy < 4.5
        store.put("h1", "v1");
        store.put("h2", "v2");
        store.put("h3", "v3");
        store.put("h4", "v4");

        assertTrue(store.isHealthy());

        store.put("h5", "v5");

        assertFalse(store.isHealthy());
    }

    @Test
    @Order(8)
    @DisplayName("Should return correct stats")
    void testStats() {
        store.put("s1", "v1");
        store.put("s2", "v2");

        LruStore.StoreStats stats = store.getStats();

        assertEquals(2, stats.currentSize());
        assertEquals(5, stats.maxSize());
        assertTrue(stats.fillPercentage() > 0);
    }

    @Test
    @Order(9)
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccess() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        String id = "c-" + threadId + "-" + i;
                        store.put(id, i);
                        store.get(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(store.size() <= 5); // max size aşılmamalı
    }

    @Test
    @Order(10)
    @DisplayName("Should shutdown gracefully")
    void testShutdown() {
        store.put("x1", "v1");
        store.put("x2", "v2");

        store.shutdown();

        assertEquals(0, store.size());
    }
}
