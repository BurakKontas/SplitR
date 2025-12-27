package tr.kontas.splitr.kafka.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.bus.QueryBus;
import tr.kontas.splitr.bus.SyncRegistry;
import tr.kontas.splitr.dto.QueryRequest;
import tr.kontas.splitr.dto.QueryResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class KafkaQueryBus implements QueryBus {

    private final KafkaTemplate<String, QueryRequest> kafka;
    private final SyncRegistry registry;
    private final ObjectMapper mapper;
    private final String callbackUrl;
    private final String queryTopic;
    private final int defaultTimeout;

    public KafkaQueryBus(String queryTopic,
                         KafkaTemplate<String, QueryRequest> kafka,
                         SyncRegistry registry,
                         ObjectMapper mapper,
                         String callbackUrl,
                         int defaultTimeout) {
        this.queryTopic = queryTopic;
        this.kafka = kafka;
        this.registry = registry;
        this.mapper = mapper;
        this.callbackUrl = callbackUrl;
        this.defaultTimeout = defaultTimeout;
    }

    public <T> T publishSync(Object query, Class<T> type, long timeoutMs) {
        try {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            var future = registry.register(id);

            kafka.send(this.queryTopic, id,
                    new QueryRequest(
                        id,
                        query.getClass().getName(),
                        mapper.writeValueAsString(query),
                        callbackUrl,
                        true,
                        now,
                        timeoutMs
                    )
            );

            return mapper.readValue(
                    future.get(timeoutMs, TimeUnit.MILLISECONDS).getResult(),
                    type
            );
        } catch (Exception e) {
            throw new RuntimeException("Query timeout", e);
        }
    }

    @Override
    public <T> T publishSync(Object query, Class<T> responseType) {
        return publishSync(query, responseType, defaultTimeout);
    }

    @Override
    public <T> CompletableFuture<QueryResponse> publishAsync(Object query, Class<T> responseType) {
        try {
            String id = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            var future = registry.register(id);

            kafka.send(this.queryTopic, id,
                new QueryRequest(
                        id,
                        query.getClass().getName(),
                        mapper.writeValueAsString(query),
                        callbackUrl,
                        false,
                        now,
                        Long.MAX_VALUE
                )
            );

            return future;
        } catch (Exception e) {
            throw new RuntimeException("Query timeout", e);
        }
    }
}