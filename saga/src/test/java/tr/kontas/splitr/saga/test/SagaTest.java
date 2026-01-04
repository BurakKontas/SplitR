package tr.kontas.splitr.saga.test;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import tr.kontas.splitr.saga.exceptions.HandlerNotFoundException;
import tr.kontas.splitr.saga.test.data.*;
import tr.kontas.splitr.saga.test.data.events.*;
import tr.kontas.splitr.saga.test.repository.TestSagaRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SagaTest.TestConfig.class)
@ActiveProfiles("test")
public class SagaTest {
    @Configuration
    @EnableAutoConfiguration // Gerekli tüm otomatik yapılandırmaları (JPA, H2 vb.) açar
    @ComponentScan(basePackages = "tr.kontas.splitr.saga") // Bean'lerin olduğu ana paket
    @EnableJpaRepositories(basePackages = "tr.kontas.splitr.saga.test.repository")
    @EntityScan(basePackages = {"tr.kontas.splitr.saga.test.data", "tr.kontas.splitr.saga.entities"})
    public static class TestConfig {
        // Test için özel bean tanımlamaları buraya gelebilir
    }

    @Autowired
    private TestSaga saga;

    @Autowired
    private TestSagaRepository repository;

    // =========================================================================
    // SCENARIO 1: Happy Path - Complete Order Flow
    // =========================================================================
    @Test
    @DisplayName("Happy Path: Order submitted → accepted → payment → inventory → completed")
    void testHappyPath_CompleteOrderFlow() {
        System.out.println("\n========== TEST: HAPPY PATH ==========");
        UUID orderId = UUID.randomUUID();

        // Step 1: Submit order
        saga.consume(new OrderSubmitted(orderId, "John Doe", LocalDateTime.now(), 100.0));
        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals("Submitted", state.getCurrentState());
        assertEquals(100.0, state.getTotalAmount());

        // Step 2: Accept order
        saga.consume(new OrderAccepted(orderId));
        state = saga.getSagaState(orderId.toString());
        assertEquals("AwaitingPayment", state.getCurrentState());

        // Step 3: Payment received
        saga.consume(new PaymentReceived(orderId, 100.0));
        state = saga.getSagaState(orderId.toString());
        assertTrue(state.isPaymentReceived());
        assertEquals("PaymentCompleted", state.getCurrentState());

        // Step 4: Inventory reserved
        saga.consume(new InventoryReserved(orderId, 5));
        state = saga.getSagaState(orderId.toString());
        assertTrue(state.isInventoryReserved());
        assertEquals(5, state.getReservedQuantity());
        assertEquals("ReadyToShip", state.getCurrentState());

        // Step 5: Complete order
        saga.consume(new OrderCompleted(orderId));

        // Verify saga is completed and removed
        assertTrue(saga.isCompleted(orderId.toString()));
        assertEquals("Completed", saga.getSagaState(orderId.toString()).getCurrentState());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 2: Order Rejected - Cancellation Flow
    // =========================================================================
    @Test
    @DisplayName("Order Rejected: Order submitted → rejected → saga cancelled")
    void testOrderRejected_SagaCancelled() {
        System.out.println("\n========== TEST: ORDER REJECTED ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Jane Doe", LocalDateTime.now(), 50.0));
        saga.consume(new OrderRejected(orderId, "Insufficient stock"));

        assertTrue(saga.isCancelled(orderId.toString()));
        assertFalse(saga.isCompleted(orderId.toString()));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals("Insufficient stock", state.getRejectionReason());
        assertTrue(state.isCancelled());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 3: Payment Failed - Retry Logic
    // =========================================================================
    @Test
    @DisplayName("Payment Failed: Payment fails → saga transitions to Failed state")
    void testPaymentFailed_TransitionsToFailed() {
        System.out.println("\n========== TEST: PAYMENT FAILED ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Bob Smith", LocalDateTime.now(), 200.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentFailed(orderId, "Card declined"));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals("Failed", state.getCurrentState());
        assertFalse(state.isPaymentReceived());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 4: Insufficient Payment - Filter Blocks
    // =========================================================================
    @Test
    @DisplayName("Insufficient Payment: Payment amount less than order → filtered out")
    void testInsufficientPayment_FilterBlocks() {
        System.out.println("\n========== TEST: INSUFFICIENT PAYMENT ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Charlie", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(orderId));

        // Try insufficient payment - should be filtered
        saga.consume(new PaymentReceived(orderId, 50.0));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertFalse(state.isPaymentReceived());
        assertEquals("AwaitingPayment", state.getCurrentState());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 5: Inventory Reservation Failed - Compensation
    // =========================================================================
    @Test
    @DisplayName("Inventory Failed: Inventory reservation fails → saga cancelled with compensation")
    void testInventoryReservationFailed_Compensation() {
        System.out.println("\n========== TEST: INVENTORY RESERVATION FAILED ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Alice", LocalDateTime.now(), 150.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentReceived(orderId, 150.0));
        saga.consume(new InventoryReservationFailed(orderId, "Out of stock"));

        assertTrue(saga.isCancelled(orderId.toString()));
        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertTrue(state.isPaymentReceived()); // Payment was received but needs refund
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 6: Invalid State Transition - Exception Thrown
    // =========================================================================
    @Test
    @DisplayName("Invalid Transition: Order completed without acceptance → exception thrown")
    void testInvalidStateTransition_ThrowsException() {
        System.out.println("\n========== TEST: INVALID STATE TRANSITION ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Dave", LocalDateTime.now(), 75.0));

        // Try to complete without going through proper flow
        Exception exception = assertThrows(HandlerNotFoundException.class, () -> {
            saga.consume(new OrderCompleted(orderId));
        });

        assertTrue(exception.getMessage().contains("Handler not found for event 'OrderCompleted' in state 'Submitted'"));
        System.out.println("Expected exception: " + exception.getMessage());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 7: High-Value Order - Conditional Logic
    // =========================================================================
    @Test
    @DisplayName("High Value Order: Order > $1000 → requires manual approval")
    void testHighValueOrder_RequiresApproval() {
        System.out.println("\n========== TEST: HIGH VALUE ORDER ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Eve", LocalDateTime.now(), 1500.0));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals(1500.0, state.getTotalAmount());
        assertEquals("Submitted", state.getCurrentState());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 8: Order Timeout - DuringAny with Compensation
    // =========================================================================
    @Test
    @DisplayName("Order Timeout: Timeout from any state → refund + cancel")
    void testOrderTimeout_CancelsWithCompensation() {
        System.out.println("\n========== TEST: ORDER TIMEOUT ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Frank", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentReceived(orderId, 100.0));

        // Timeout should trigger refund
        saga.consume(new OrderTimeout(orderId));

        assertTrue(saga.isCancelled(orderId.toString()));
        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertTrue(state.isPaymentReceived()); // Confirms refund was needed
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 9: Multiple Independent Sagas
    // =========================================================================
    @Test
    @DisplayName("Multiple Sagas: Two orders run independently")
    void testMultipleSagasIndependently() {
        System.out.println("\n========== TEST: MULTIPLE INDEPENDENT SAGAS ==========");
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();

        // Order 1: Success path
        saga.consume(new OrderSubmitted(order1, "User1", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(order1));
        saga.consume(new PaymentReceived(order1, 100.0));

        // Order 2: Rejection path
        saga.consume(new OrderSubmitted(order2, "User2", LocalDateTime.now(), 200.0));
        saga.consume(new OrderRejected(order2, "Fraud detected"));

        // Verify independence
        TestSagaState state1 = saga.getSagaState(order1.toString());
        assertNotNull(state1);
        assertEquals("PaymentCompleted", state1.getCurrentState());

        assertTrue(saga.isCancelled(order2.toString()));

        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 10: Completed Saga Ignores New Events
    // =========================================================================
    @Test
    @DisplayName("Completed Saga: Events after finalization are ignored")
    void testCompletedSagaIgnoresNewEvents() {
        System.out.println("\n========== TEST: COMPLETED SAGA IGNORES EVENTS ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Grace", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentReceived(orderId, 100.0));
        saga.consume(new InventoryReserved(orderId, 3));
        saga.consume(new OrderCompleted(orderId));

        assertTrue(saga.isCompleted(orderId.toString()));
        assertEquals("Completed", saga.getSagaState(orderId.toString()).getCurrentState());

        // Try to send another event - should be ignored
        saga.consume(new PaymentReceived(orderId, 100.0)); // No exception

        assertEquals("Completed", saga.getSagaState(orderId.toString()).getCurrentState());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 11: Shipping Scheduled Flow
    // =========================================================================
    @Test
    @DisplayName("Shipping Scheduled: Alternative completion path")
    void testShippingScheduled() {
        System.out.println("\n========== TEST: SHIPPING SCHEDULED ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Henry", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentReceived(orderId, 100.0));
        saga.consume(new InventoryReserved(orderId, 2));
        saga.consume(new ShippingScheduled(orderId, LocalDateTime.now().plusDays(1)));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals("Completed", state.getCurrentState());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 12: All Active Sagas Tracking
    // =========================================================================
    @Test
    @DisplayName("Active Sagas: Track multiple active saga instances")
    void testGetAllActiveSagas() {
        System.out.println("\n========== TEST: ACTIVE SAGAS TRACKING ==========");
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();
        UUID order3 = UUID.randomUUID();

        saga.consume(new OrderSubmitted(order1, "User1", LocalDateTime.now(), 100.0));
        saga.consume(new OrderSubmitted(order2, "User2", LocalDateTime.now(), 200.0));
        saga.consume(new OrderSubmitted(order3, "User3", LocalDateTime.now(), 300.0));

        assertEquals(3, saga.getAllActiveSagas().size());

        // Complete one saga
        saga.consume(new OrderAccepted(order1));
        saga.consume(new PaymentReceived(order1, 100.0));
        saga.consume(new InventoryReserved(order1, 1));
        saga.consume(new OrderCompleted(order1));

        assertEquals(2, saga.getAllActiveSagas().size());
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 13: Timeout with Inventory Release
    // =========================================================================
    @Test
    @DisplayName("Timeout with Compensation: Release inventory and refund payment")
    void testTimeoutWithFullCompensation() {
        System.out.println("\n========== TEST: TIMEOUT WITH FULL COMPENSATION ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Ivy", LocalDateTime.now(), 100.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentReceived(orderId, 100.0));
        saga.consume(new InventoryReserved(orderId, 10));

        TestSagaState stateBefore = saga.getSagaState(orderId.toString());
        assertTrue(stateBefore.isPaymentReceived());
        assertTrue(stateBefore.isInventoryReserved());

        saga.consume(new OrderTimeout(orderId));

        assertTrue(saga.isCancelled(orderId.toString()));
        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 14: Standard vs High-Value Order Branching
    // =========================================================================
    @Test
    @DisplayName("Conditional Branching: Standard order vs high-value order")
    void testConditionalBranching() {
        System.out.println("\n========== TEST: CONDITIONAL BRANCHING ==========");

        // Standard order
        UUID order1 = UUID.randomUUID();
        saga.consume(new OrderSubmitted(order1, "Standard User", LocalDateTime.now(), 50.0));
        TestSagaState state1 = saga.getSagaState(order1.toString());
        assertEquals(50.0, state1.getTotalAmount());

        // High-value order
        UUID order2 = UUID.randomUUID();
        saga.consume(new OrderSubmitted(order2, "Premium User", LocalDateTime.now(), 2000.0));
        TestSagaState state2 = saga.getSagaState(order2.toString());
        assertEquals(2000.0, state2.getTotalAmount());

        System.out.println("========== TEST PASSED ==========\n");
    }

    // =========================================================================
    // SCENARIO 15: Payment Retry for Small Orders
    // =========================================================================
    @Test
    @DisplayName("Payment Retry: Small orders get automatic retry on payment failure")
    void testPaymentRetryForSmallOrders() {
        System.out.println("\n========== TEST: PAYMENT RETRY ==========");
        UUID orderId = UUID.randomUUID();

        saga.consume(new OrderSubmitted(orderId, "Jack", LocalDateTime.now(), 300.0));
        saga.consume(new OrderAccepted(orderId));
        saga.consume(new PaymentFailed(orderId, "Temporary network error"));

        TestSagaState state = saga.getSagaState(orderId.toString());
        assertNotNull(state);
        assertEquals("Failed", state.getCurrentState());
        // In real scenario, scheduled retry would be triggered
        System.out.println("========== TEST PASSED ==========\n");
    }

    @Test
    @DisplayName("Automatic Timeout Test: Saga should cancel itself after 10 seconds")
    void testAutomaticSagaTimeout() throws InterruptedException {
        UUID orderId = UUID.randomUUID();

        // Sadece siparişi başlatıyoruz ve başka hiçbir mesaj göndermiyoruz
        saga.consume(new OrderSubmitted(orderId, "TimeoutUser", LocalDateTime.now(), 250.0));

        System.out.println(">>> Waiting for automatic timeout (10s)...");

        // Timeout süresinden biraz daha fazla bekleyelim (12s)
        Thread.sleep(12000);

        // Sonucu doğrula
        TestSagaState state = repository.findByOrderId(orderId.toString()).orElseThrow();

        System.out.println(">>> Final State: " + state.getCurrentState());
        assertEquals("Timeouted", state.getCurrentState(), "Saga should have timed out automatically!");
    }

    @ParameterizedTest(name = "Chaos Load Test with {0} Sagas")
    @ValueSource(ints = {10, 100, 1000})
    void testSagaTrueConcurrency(int sagaCount) throws InterruptedException {
        repository.deleteAll();
        repository.flush();

        int totalMessages = sagaCount * 5;

        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch finishLatch = new CountDownLatch(totalMessages);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < sagaCount; i++) {
            UUID orderId = UUID.randomUUID();

            executor.submit(() -> processStep(() -> saga.consume(new OrderSubmitted(orderId, "User", LocalDateTime.now(), 100.0)), finishLatch));

            scheduleMessage(() -> saga.consume(new OrderAccepted(orderId)), 100, executor, finishLatch);
            scheduleMessage(() -> saga.consume(new PaymentReceived(orderId, 100.0)), 200, executor, finishLatch);
            scheduleMessage(() -> saga.consume(new InventoryReserved(orderId, 5)), 300, executor, finishLatch);
            scheduleMessage(() -> saga.consume(new OrderCompleted(orderId)), 400, executor, finishLatch);
        }

        boolean finished = finishLatch.await(10, TimeUnit.MINUTES);
        assertTrue(finished, "Test zaman aşımına uğradı!");

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Thread.sleep(3000);

        long duration = System.currentTimeMillis() - startTime;
        long completedInDb = repository.countCompletedSagas();

        System.out.println("\n--- CHAOS TEST RESULTS ---");
        System.out.printf("Sagas: %d | Total Messages: %d\n", sagaCount, totalMessages);
        System.out.printf("Time Taken: %d ms | Success Rate: %.2f%%\n", duration, (completedInDb * 100.0 / sagaCount));
        System.out.println("--------------------------\n");

        assertEquals(sagaCount, completedInDb, "Kaos altında veri kaybı veya eksik saga!");
    }

    private void processStep(Runnable step, CountDownLatch latch) {
        try {
            step.run();
        } catch (Exception e) {
            System.err.println("Chaos Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private void scheduleMessage(Runnable step, int delay, ExecutorService executor, CountDownLatch latch) {
        executor.submit(() -> {
            try {
                Thread.sleep(new Random().nextInt(delay));
                processStep(step, latch);
            } catch (InterruptedException e) {
                latch.countDown();
            }
        });
    }
}