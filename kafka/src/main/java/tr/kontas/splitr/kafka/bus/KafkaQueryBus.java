package tr.kontas.splitr.kafka.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.bus.QueryBus;
import tr.kontas.splitr.bus.SyncRegistry;
import tr.kontas.splitr.dto.QueryRequest;

import java.util.concurrent.CompletableFuture;

public class KafkaQueryBus extends AbstractKafkaBus<QueryRequest> implements QueryBus {

    public KafkaQueryBus(String queryTopic, KafkaTemplate<String, QueryRequest> kafka,
                         SyncRegistry registry, ObjectMapper mapper,
                         String callbackUrl, int defaultTimeout) {
        super(queryTopic, kafka, registry, mapper, callbackUrl, defaultTimeout);
    }

    @Override
    protected QueryRequest createRequest(String id, String typeName, String payload, boolean isSync, long now, long timeout) {
        return new QueryRequest(id, typeName, payload, callbackUrl, isSync, now, timeout);
    }

    @Override
    public <T> T publishSync(Object query, Class<T> responseType, long timeoutMs) {
        return executeSync(query, responseType, timeoutMs);
    }

    @Override
    public <T> T publishSync(Object query, Class<T> responseType) {
        return publishSync(query, responseType, defaultTimeout);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Object query, Class<T> responseType) {
        return executeAsync(query, responseType, false);
    }
}