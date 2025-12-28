package tr.kontas.splitr.rabbitmq.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tr.kontas.splitr.bus.query.Query;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.dto.QueryRequest;
import tr.kontas.splitr.rabbitmq.bus.base.AbstractRabbitBus;

import java.util.concurrent.CompletableFuture;

public class RabbitQueryBus extends AbstractRabbitBus<QueryRequest> implements QueryBus {

    public RabbitQueryBus(String queue, RabbitTemplate rabbit,
                          SyncRegistry registry, ObjectMapper mapper,
                          String callbackUrl, int defaultTimeout) {
        super(queue, rabbit, registry, mapper, callbackUrl, defaultTimeout);
    }

    @Override
    protected QueryRequest createRequest(String id, String typeName, String payload,
                                         boolean isSync, long now, long timeout) {
        return new QueryRequest(id, typeName, payload, callbackUrl, isSync, now, timeout);
    }

    @Override
    public <T> T publishSync(Query query, Class<T> responseType, long timeoutMs) {
        return executeSync(query, responseType, timeoutMs);
    }

    @Override
    public <T> T publishSync(Query query, Class<T> responseType) {
        return publishSync(query, responseType, defaultTimeout);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Query query, Class<T> responseType) {
        return executeAsync(query, responseType);
    }
}

