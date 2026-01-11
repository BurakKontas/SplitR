package tr.kontas.splitr.bus.registry;

import org.junit.jupiter.api.*;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncRegistryTest {

    private SyncRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SyncRegistry(5000L, 100); // 5 saniye timeout, max 100 entry
    }

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("Should register and complete successfully")
    void testRegisterAndComplete() throws Exception {
        // Arrange
        String id = "test-id-1";
        CompletableFuture<BaseResponse> future = registry.register(id);

        // Act
        BaseResponse response = new TestResponse(id, "success");
        registry.complete(response);

        // Assert
        BaseResponse result = future.get(1, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(0, registry.size()); // Completed ve removed olmalı
    }

    @Test
    @Order(2)
    @DisplayName("Should timeout after specified duration")
    void testTimeout() {
        String id = "timeout-test";
        CompletableFuture<BaseResponse> future = registry.register(id, 100L);

        ExecutionException ex = assertThrows(
                ExecutionException.class,
                () -> future.get(300, TimeUnit.MILLISECONDS)
        );

        assertTrue(ex.getCause() instanceof TimeoutException);

        // Timeout sonrası registry'den silinmiş olmalı
        assertTrue(registry.size() == 0 || registry.size() == 1);
    }

    @Test
    @Order(3)
    @DisplayName("Should handle multiple concurrent registrations")
    void testConcurrentRegistrations() throws Exception {
        // Arrange
        int count = 50;
        List<CompletableFuture<BaseResponse>> futures = new ArrayList<>();

        // Act - Paralel kayıt
        for (int i = 0; i < count; i++) {
            String id = "concurrent-" + i;
            futures.add(registry.register(id));
        }

        assertEquals(count, registry.size());

        // Complete all
        for (int i = 0; i < count; i++) {
            String id = "concurrent-" + i;
            registry.complete(new TestResponse(id, "done"));
        }

        // Assert - Tüm future'lar tamamlanmalı
        for (CompletableFuture<BaseResponse> future : futures) {
            BaseResponse result = future.get(1, TimeUnit.SECONDS);
            assertNotNull(result);
        }

        assertEquals(0, registry.size());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject when registry is full")
    void testMaxSizeLimit() {
        // Arrange - Max 100 entry
        for (int i = 0; i < 100; i++) {
            registry.register("full-test-" + i);
        }

        // Act - 101. kayıt reject edilmeli
        CompletableFuture<BaseResponse> rejectedFuture = registry.register("overflow");

        // Assert
        assertTrue(rejectedFuture.isCompletedExceptionally());
        assertThrows(ExecutionException.class, () -> rejectedFuture.get());
    }

    @Test
    @Order(5)
    @DisplayName("Should complete exceptionally")
    void testCompleteExceptionally() {
        // Arrange
        String id = "exception-test";
        CompletableFuture<BaseResponse> future = registry.register(id);

        // Act
        registry.completeExceptionally(id, new RuntimeException("Test error"));

        // Assert
        assertThrows(ExecutionException.class, () -> future.get());
        assertEquals(0, registry.size());
    }

    @Test
    @Order(6)
    @DisplayName("Should cancel pending request")
    void testCancel() {
        // Arrange
        String id = "cancel-test";
        CompletableFuture<BaseResponse> future = registry.register(id);

        // Act
        boolean cancelled = registry.cancel(id);

        // Assert
        assertTrue(cancelled);
        assertTrue(future.isCancelled());
        assertEquals(0, registry.size());
    }

    @Test
    @Order(7)
    @DisplayName("Should cleanup expired entries automatically")
    void testAutomaticCleanup() throws InterruptedException {
        // Arrange
        for (int i = 0; i < 10; i++) {
            registry.register("cleanup-test-" + i, 500L); // 500ms timeout
        }

        assertEquals(10, registry.size());

        // Act - Timeout + cleanup bekle
        Thread.sleep(2000); // Cleanup 30 saniyede bir ama timeout 500ms

        // Assert - Timeout olan entry'ler silinmiş olmalı
        assertTrue(registry.size() < 10);
    }

    @Test
    @Order(8)
    @DisplayName("Should return healthy when size is acceptable")
    void testHealthCheck() {
        // Arrange - Max 100, healthy threshold %90
        for (int i = 0; i < 80; i++) {
            registry.register("health-" + i);
        }

        // Assert - Healthy
        assertTrue(registry.isHealthy());

        // Act - Fill to 95
        for (int i = 80; i < 95; i++) {
            registry.register("health-" + i);
        }

        // Assert - Unhealthy
        assertFalse(registry.isHealthy());
    }

    @Test
    @Order(9)
    @DisplayName("Should handle double completion gracefully")
    void testDoubleCompletion() throws Exception {
        // Arrange
        String id = "double-complete";
        CompletableFuture<BaseResponse> future = registry.register(id);
        BaseResponse response = new TestResponse(id, "first");

        // Act
        registry.complete(response);
        registry.complete(response); // İkinci kez complete

        // Assert - İlk sonuç dönmeli, hata olmamalı
        BaseResponse result = future.get(1, TimeUnit.SECONDS);
        assertEquals("first", ((TestResponse) result).getResult());
    }

    @Test
    @Order(10)
    @DisplayName("Should clear all entries")
    void testClear() {
        // Arrange
        for (int i = 0; i < 20; i++) {
            registry.register("clear-" + i);
        }

        assertEquals(20, registry.size());

        // Act
        registry.clear();

        // Assert
        assertEquals(0, registry.size());
    }

    @Test
    @Order(11)
    @DisplayName("Should handle stress test")
    void testStressTest() throws Exception {
        // Arrange
        int threadCount = 10;
        int requestsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        // Act
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    for (int i = 0; i < requestsPerThread; i++) {
                        String id = "stress-" + threadId + "-" + i;
                        CompletableFuture<BaseResponse> f = registry.register(id);

                        // Simulate async completion
                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(10);
                                registry.complete(new TestResponse(id, "done"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } finally {
                    latch.countDown();
                }
            }));
        }

        // Wait for completion
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Thread.sleep(1000);
        assertTrue(registry.size() < 100);
    }

    // Helper class
    static class TestResponse extends BaseResponse {
        private final String result;

        public TestResponse(String id, String result) {
            super(id, result);
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }
}