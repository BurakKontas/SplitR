package tr.kontas.splitr.kafka.bus.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.bus.base.IdempotencyProtected;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.dto.base.BaseResponse;

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

    protected <T> T executeSync(IdempotencyProtected payload, Class<T> responseType, long timeoutMs) {
        try {
            String id = payload.getIdempotencyKey();
            var future = registry.register(id);

            sendInternal(payload, true, timeoutMs);

            BaseResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return mapper.readValue(response.getResult(), responseType);
        } catch (Exception e) {
            throw new RuntimeException("Sync execution failed or timeout", e);
        }
    }

    // Event bus için
    protected void execute(IdempotencyProtected payload) {
        try {
            sendInternal(payload, false, Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException("Async execution failed", e);
        }
    }

    protected <T> CompletableFuture<T> executeAsync(IdempotencyProtected payload, Class<T> responseType) {
        try {
            String id = payload.getIdempotencyKey();
            var future = registry.register(id);

            sendInternal(payload, false, Long.MAX_VALUE);

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

    protected void sendInternal(IdempotencyProtected payload, boolean isSync, long timeoutMs) throws Exception {
        TRequest request = createRequest(
                payload.getIdempotencyKey(),
                payload.getClass().getName(),
                mapper.writeValueAsString(payload),
                isSync,
                System.currentTimeMillis(),
                timeoutMs
        );
        kafka.send(this.topic, payload.getIdempotencyKey(), request);
    }
}