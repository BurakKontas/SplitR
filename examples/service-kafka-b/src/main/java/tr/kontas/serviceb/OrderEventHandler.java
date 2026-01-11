package tr.kontas.serviceb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.bus.base.BaseEventHandler;
import tr.kontas.splitr.test.OrderProcessedEvent;

@Component
@Slf4j
public class OrderEventHandler extends BaseEventHandler<OrderProcessedEvent> {
    @Override
    public void onEvent(OrderProcessedEvent payload) {
        log.atInfo().log("OrderProcessedEvent in OrderEventHandler processed with id: " + payload.getOrderId());
    }
}
