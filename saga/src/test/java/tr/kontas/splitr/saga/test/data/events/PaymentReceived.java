package tr.kontas.splitr.saga.test.data.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PaymentReceived {
    private UUID orderId;
    private double amount;
}
