package tr.kontas.splitr.rabbitmq.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tr.kontas.splitr.bus.event.Event;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.dto.EventRequest;
import tr.kontas.splitr.rabbitmq.bus.base.AbstractRabbitBus;

public class RabbitEventBus extends AbstractRabbitBus<EventRequest> implements EventBus {

    public RabbitEventBus(String queue, RabbitTemplate rabbit,
                          SyncRegistry registry, ObjectMapper mapper,
                          String callbackUrl, int defaultTimeout) {
        super(queue, rabbit, registry, mapper, callbackUrl, defaultTimeout);
    }

    @Override
    protected EventRequest createRequest(String id, String typeName, String payload,
                                         boolean isSync, long now, long timeout) {
        return new EventRequest(id, typeName, payload);
    }

    @Override
    public void publish(Event event) {
        execute(event);
    }
}

