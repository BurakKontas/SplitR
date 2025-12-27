package tr.kontas.splitr.kafka.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.bus.SyncRegistry;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractKafkaBus<TRequest> {

    protected final KafkaTemplate<String, TRequest> kafka;
    protected final SyncRegistry registry;
    protected final ObjectMapper mapper;
    protected final String callbackUrl;
    protected final String topic;
    protected final int defaultTimeout;

    protected AbstractKafkaBus(String topic,
                               KafkaTemplate<String, TRequest> kafka,
                               SyncRegistry registry,
                               ObjectMapper mapper,
                               String callbackUrl,
                               int defaultTimeout) {
        this.topic = topic;
        this.kafka = kafka;
        this.registry = registry;
        this.mapper = mapper;
        this.callbackUrl = callbackUrl;
        this.defaultTimeout = defaultTimeout;

        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new RuntimeException("splitr.callback-url is blank");
        }
    }

    /**
     * Alt sınıflar kendi Request tiplerini (CommandRequest/QueryRequest) burada oluşturur.
     */
    protected abstract TRequest createRequest(String id, String typeName, String payload,
                                              boolean isSync, long now, long timeout);

    protected <T> T executeSync(Object payload, Class<T> responseType, long timeoutMs) {
        try {
            String id = UUID.randomUUID().toString();
            var future = registry.registerQuery(id);

            sendInternal(id, payload, true, timeoutMs);

            BaseResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return mapper.readValue(response.getResult(), responseType);
        } catch (Exception e) {
            throw new RuntimeException("Sync execution failed or timeout", e);
        }
    }

    protected <T> CompletableFuture<T> executeAsync(Object payload, Class<T> responseType, boolean isCommand) {
        try {
            String id = UUID.randomUUID().toString();
            // Registry ayrımı: Command ise registerCommand, Query ise registerQuery
            var future = isCommand ? registry.registerCommand(id) : registry.registerQuery(id);

            sendInternal(id, payload, false, Long.MAX_VALUE);

            return future.thenApply(response -> {
                try {
                    return mapper.readValue(response.getResult(), responseType);
                } catch (Exception e) {
                    throw new RuntimeException("Deserialization failed", e);
                }
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void sendInternal(String id, Object payload, boolean isSync, long timeoutMs) throws Exception {
        TRequest request = createRequest(
                id,
                payload.getClass().getName(),
                mapper.writeValueAsString(payload),
                isSync,
                System.currentTimeMillis(),
                timeoutMs
        );
        kafka.send(this.topic, id, request);
    }
}