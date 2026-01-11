package tr.kontas.splitr.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.bus.event.BaseEvent;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderProcessedEvent extends BaseEvent {
    private String orderId;
}
