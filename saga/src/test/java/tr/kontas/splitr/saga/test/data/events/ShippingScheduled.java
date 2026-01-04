package tr.kontas.splitr.saga.test.data.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ShippingScheduled {
    private UUID orderId;
    private LocalDateTime scheduledDate;
}
