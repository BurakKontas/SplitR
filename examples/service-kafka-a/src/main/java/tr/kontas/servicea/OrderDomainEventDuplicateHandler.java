package tr.kontas.servicea;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.bus.base.BaseDomainEventHandler;
import tr.kontas.splitr.test.OrderDomainEvent;

@Slf4j
@Component
public class OrderDomainEventDuplicateHandler extends BaseDomainEventHandler<OrderDomainEvent> {
    @Override
    public void onEvent(OrderDomainEvent payload) {
        log.atInfo().log("OrderDomainEventDuplicateHandler triggered with id: " + payload.getId());
    }
}
