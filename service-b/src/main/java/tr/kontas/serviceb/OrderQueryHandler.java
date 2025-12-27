package tr.kontas.serviceb;

import org.springframework.stereotype.Component;
import tr.kontas.splitr.consumer.QueryHandler;
import tr.kontas.splitr.test.OrderQuery;

@Component
public class OrderQueryHandler implements QueryHandler<OrderQuery> {

    public Class<OrderQuery> type() { return OrderQuery.class; }

    public Object handle(OrderQuery q) {
        return "ORDER-" + q.orderId();
    }
}
