package tr.kontas.splitr.saga.test.data.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class InventoryReservationFailed {
    private UUID orderId;
    private String reason;
}
