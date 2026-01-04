package tr.kontas.splitr.saga.test.data.events;

import lombok.Getter;

import java.util.UUID;

import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderSubmitted {
    private UUID orderId;
    private String customerName;
    private LocalDateTime timestamp;
    private double amount;
}

