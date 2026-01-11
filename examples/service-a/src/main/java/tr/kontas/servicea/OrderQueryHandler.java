package tr.kontas.servicea;

import org.springframework.stereotype.Component;
import tr.kontas.splitr.bus.base.BaseQueryHandler;
import tr.kontas.splitr.test.OrderQuery;

@Component
public class OrderQueryHandler extends BaseQueryHandler<OrderQuery> {
    @Override
    public Object handle(OrderQuery q) {
        return "DATA-FOR-" + q.getOrderId();
    }
}

