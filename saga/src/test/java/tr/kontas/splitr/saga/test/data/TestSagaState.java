package tr.kontas.splitr.saga.test.data;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import tr.kontas.splitr.saga.entities.abstracts.BaseSagaState;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TestSagaState extends BaseSagaState {
    private String orderId;
    private String customerName;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private double totalAmount;
    private boolean paymentReceived;
    private boolean inventoryReserved;
    private int reservedQuantity;
    private String rejectionReason;
}
