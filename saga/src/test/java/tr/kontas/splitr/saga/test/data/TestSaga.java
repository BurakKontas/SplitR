package tr.kontas.splitr.saga.test.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.saga.annotations.Saga;
import tr.kontas.splitr.saga.entities.Event;
import tr.kontas.splitr.saga.entities.State;
import tr.kontas.splitr.saga.entities.abstracts.BaseSaga;
import tr.kontas.splitr.saga.test.data.events.*;
import tr.kontas.splitr.saga.test.repository.TestSagaRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Saga(
    retryWithJitter = true,
    retryAttempts = 50
)
@Component
public class TestSaga extends BaseSaga<TestSagaState> {
    // Events
    private final Event<OrderSubmitted> OrderSubmittedEvent = new Event<>(OrderSubmitted.class);
    private final Event<OrderAccepted> OrderAcceptedEvent = new Event<>(OrderAccepted.class);
    private final Event<OrderRejected> OrderRejectedEvent = new Event<>(OrderRejected.class);
    private final Event<OrderCompleted> OrderCompletedEvent = new Event<>(OrderCompleted.class);
    private final Event<PaymentReceived> PaymentReceivedEvent = new Event<>(PaymentReceived.class);
    private final Event<PaymentFailed> PaymentFailedEvent = new Event<>(PaymentFailed.class);
    private final Event<InventoryReserved> InventoryReservedEvent = new Event<>(InventoryReserved.class);
    private final Event<InventoryReservationFailed> InventoryReservationFailedEvent = new Event<>(InventoryReservationFailed.class);
    private final Event<ShippingScheduled> ShippingScheduledEvent = new Event<>(ShippingScheduled.class);
    private final Event<OrderTimeout> OrderTimeoutEvent = new Event<>(OrderTimeout.class);

    // States
    private final State Submitted = new State("Submitted");
    private final State AwaitingPayment = new State("AwaitingPayment");
    private final State PaymentCompleted = new State("PaymentCompleted");
    private final State ReadyToShip = new State("ReadyToShip");
    private final State Rejected = new State("Rejected");
    private final State Failed = new State("Failed");

    @Autowired
    public TestSaga(TestSagaRepository repository) {
        super(repository);

        // Configure strict state transitions
        enableStrictTransitions();
        configureAllowedTransitions();

        // Register all events with correlation
        Event(() -> OrderSubmittedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> OrderAcceptedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> OrderRejectedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> OrderCompletedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> PaymentReceivedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> PaymentFailedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> InventoryReservedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> InventoryReservationFailedEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> ShippingScheduledEvent, x -> x.correlateById(o -> o.getOrderId().toString()));
        Event(() -> OrderTimeoutEvent, x -> x.correlateById(o -> o.getOrderId().toString()));

        // =====================================================================
        // SCENARIO 1: Initially - Order Submitted (Happy Path Start)
        // =====================================================================
        Initially(
                When(() -> OrderSubmittedEvent)
                        .Then((context, instance) -> {
                            instance.setOrderId(context.getData().getOrderId().toString());
                            instance.setCustomerName(context.getData().getCustomerName());
                            instance.setSubmittedAt(context.getData().getTimestamp());
                            instance.setTotalAmount(context.getData().getAmount());
                            System.out.println("[SAGA] Order " + context.getData().getOrderId() + " submitted by " +
                                    context.getData().getCustomerName() + " - Amount: $" + context.getData().getAmount());
                        })
                        // Conditional: High-value orders require manual approval
                        .IfElse(
                                (context, instance) -> context.getData().getAmount() > 1000,
                                then -> then.Then((c, i) -> {
                                    System.out.println("[SAGA] ⚠️  High-value order - requires manual approval");
                                }),
                                els -> els.Then((c, i) -> {
                                    System.out.println("[SAGA] ✓ Standard order - auto-approved");
                                })
                        )
                        .Schedule((context, instance) ->
                                new OrderTimeout(context.getData().getOrderId()), 10000)
                        .TransitionTo(Submitted)
        );

        // =====================================================================
        // SCENARIO 2: Order Accepted (Happy Path Continuation)
        // =====================================================================
        During(Submitted,
                When(() -> OrderAcceptedEvent)
                        .Then((context, instance) -> {
                            System.out.println("[SAGA] ✓ Order " + context.getData().getOrderId() + " accepted");
                        })
                        .ThenAsync((context, instance) -> {
                            System.out.println("[SAGA] → Initiating payment processing...");
                            return CompletableFuture.completedFuture(null);
                        })
                        .TransitionTo(AwaitingPayment)
        );

        // =====================================================================
        // SCENARIO 3: Order Rejected (Cancellation Path)
        // =====================================================================
        During(Submitted,
                When(() -> OrderRejectedEvent)
                        .Then((context, instance) -> {
                            instance.setRejectionReason(context.getData().getReason());
                            System.out.println("[SAGA] ✗ Order " + context.getData().getOrderId() + " rejected: " +
                                    context.getData().getReason());
                        })
                        .Cancel()
                        .TransitionTo(Rejected)
        );

        // =====================================================================
        // SCENARIO 4: Payment Received with Validation (Happy Path)
        // =====================================================================
        During(AwaitingPayment,
                When(() -> PaymentReceivedEvent)
                        .Filter((context, instance) -> {
                            boolean valid = context.getData().getAmount() >= instance.getTotalAmount();
                            if (!valid) {
                                System.out.println("[SAGA] ✗ Insufficient payment: $" + context.getData().getAmount() +
                                        " (required: $" + instance.getTotalAmount() + ")");
                            }
                            return valid;
                        })
                        .Then((context, instance) -> {
                            instance.setPaymentReceived(true);
                            System.out.println("[SAGA] ✓ Payment received: $" + context.getData().getAmount());
                        })
                        .ThenAsync((context, instance) -> {
                            System.out.println("[SAGA] → Processing payment verification...");
                            return CompletableFuture.runAsync(() -> {
                                try { Thread.sleep(50); } catch (InterruptedException e) {}
                            });
                        })
                        .Publish((context, instance) -> {
                            System.out.println("[SAGA] → Publishing InventoryReserved event");
                            return new InventoryReserved(context.getData().getOrderId(), 1);
                        })
                        .TransitionTo(PaymentCompleted)
        );

        // =====================================================================
        // SCENARIO 5: Payment Failed with Retry Logic
        // =====================================================================
        During(AwaitingPayment,
                When(() -> PaymentFailedEvent)
                        .Then((context, instance) -> {
                            System.out.println("[SAGA] ✗ Payment failed: " + context.getData().getReason());
                        })
                        .If(
                                (context, instance) -> instance.getTotalAmount() < 500,
                                retry -> retry
                                        .Then((c, i) -> System.out.println("[SAGA] → Scheduling payment retry..."))
                                        .Schedule((c, i) ->
                                                new PaymentReceived(c.getData().getOrderId(), i.getTotalAmount()), 5000)
                        )
                        .TransitionTo(Failed)
        );

        // =====================================================================
        // SCENARIO 6: Inventory Reserved (Happy Path)
        // =====================================================================
        During(PaymentCompleted,
                When(() -> InventoryReservedEvent)
                        .Then((context, instance) -> {
                            instance.setInventoryReserved(true);
                            instance.setReservedQuantity(context.getData().getQuantity());
                            System.out.println("[SAGA] ✓ Inventory reserved: " + context.getData().getQuantity() + " items");
                        })
                        .TransitionTo(ReadyToShip)
        );

        // =====================================================================
        // SCENARIO 7: Inventory Reservation Failed (Compensation)
        // =====================================================================
        During(PaymentCompleted,
                When(() -> InventoryReservationFailedEvent)
                        .Then((context, instance) -> {
                            System.out.println("[SAGA] ✗ Inventory reservation failed: " + context.getData().getReason());
                            System.out.println("[SAGA] → Initiating compensation: refunding payment");
                        })
                        .Publish((context, instance) -> {
                            System.out.println("[SAGA] → Publishing OrderCancelled event");
                            return new OrderCancelled(UUID.fromString(instance.getOrderId()), "Out of stock");
                        })
                        .Cancel()
                        .TransitionTo(Failed)
        );

        // =====================================================================
        // SCENARIO 8: Shipping Scheduled
        // =====================================================================
        During(ReadyToShip,
                When(() -> ShippingScheduledEvent)
                        .Then((context, instance) -> {
                            System.out.println("[SAGA] ✓ Shipping scheduled for " + context.getData().getScheduledDate());
                        })
                        .TransitionTo(Completed)
        );

        // =====================================================================
        // SCENARIO 9: Order Completed (Final Happy Path)
        // =====================================================================
        During(ReadyToShip,
                When(() -> OrderCompletedEvent)
                        .Then((context, instance) -> {
                            instance.setCompletedAt(LocalDateTime.now());
                            System.out.println("[SAGA] ✓✓✓ Order " + context.getData().getOrderId() + " COMPLETED!");
                        })
                        .ThenAsync((context, instance) -> {
                            System.out.println("[SAGA] → Sending completion notifications...");
                            return CompletableFuture.completedFuture(null);
                        })
                        .Publish((context, instance) -> {
                            System.out.println("[SAGA] → Publishing final OrderCompleted event");
                            return new OrderCompleted(context.getData().getOrderId());
                        })
                        .TransitionTo(Completed)
                        .Finalize()
        );

        // =====================================================================
        // SCENARIO 10: Timeout - Can happen from any state (Compensation)
        // =====================================================================
        DuringAny(
                When(() -> OrderTimeoutEvent)
                        .Then((context, instance) -> {
                            System.out.println("[SAGA] ⏱️  Order " + context.getData().getOrderId() + " TIMED OUT");
                        })
                        // Conditional refund if payment was received
                        .If(
                                (context, instance) -> instance.isPaymentReceived(),
                                refund -> refund.Then((c, i) -> {
                                    System.out.println("[SAGA] → Refunding payment for timed out order");
                                    System.out.println("[SAGA] → Payment refund completed: $" + i.getTotalAmount());
                                })
                        )
                        // Release inventory if it was reserved
                        .If(
                                (context, instance) -> instance.isInventoryReserved(),
                                release -> release.Then((c, i) -> {
                                    System.out.println("[SAGA] → Releasing reserved inventory: " + i.getReservedQuantity() + " items");
                                })
                        )
                        .Cancel()
                        .TransitionTo(Timeouted)
        );
    }

    /**
     * Configure all allowed state transitions for strict validation
     */
    private void configureAllowedTransitions() {
        // From Initial (null)
        allowTransition(null, Submitted);

        // From Submitted
        allowTransition(Submitted, AwaitingPayment);
        allowTransition(Submitted, Rejected);
        allowTransition(Submitted, Cancelled);
        allowTransition(Submitted, Timeouted);

        // From AwaitingPayment
        allowTransition(AwaitingPayment, PaymentCompleted);
        allowTransition(AwaitingPayment, Failed);
        allowTransition(AwaitingPayment, Cancelled);
        allowTransition(AwaitingPayment, Timeouted);

        // From PaymentCompleted
        allowTransition(PaymentCompleted, ReadyToShip);
        allowTransition(PaymentCompleted, Failed);
        allowTransition(PaymentCompleted, Cancelled);
        allowTransition(PaymentCompleted, Timeouted);

        // From ReadyToShip
        allowTransition(ReadyToShip, Completed);
        allowTransition(ReadyToShip, Cancelled);
        allowTransition(ReadyToShip, Timeouted);

        // From Failed (can be retried or cancelled)
        allowTransition(Failed, Cancelled);
        allowTransition(Failed, Timeouted);
    }

    @Override
    protected TestSagaState createNewState() {
        return new TestSagaState();
    }
}