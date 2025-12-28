package tr.kontas.splitr.rabbitmq.bus.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRabbitBus<TRequest> {

    protected final RabbitTemplate rabbit;
    protected final SyncRegistry registry;
    protected final ObjectMapper mapper;
    protected final String callbackUrl;
    protected final String queue;
    protected final int defaultTimeout;

    protected AbstractRabbitBus(String queue,
                                RabbitTemplate rabbit,
                                SyncRegistry registry,
                                ObjectMapper mapper,
                                String callbackUrl,
                                int defaultTimeout) {
        this.queue = queue;
        this.rabbit = rabbit;
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
            var future = registry.register(id);

            sendInternal(id, payload, true, timeoutMs);

            BaseResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return mapper.readValue(response.getResult(), responseType);
        } catch (Exception e) {
            throw new RuntimeException("Sync execution failed or timeout", e);
        }
    }

    // Event bus için
    protected void execute(Object payload) {
        try {
            String id = UUID.randomUUID().toString();
            sendInternal(id, payload, false, Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException("Async execution failed", e);
        }
    }

    protected <T> CompletableFuture<T> executeAsync(Object payload, Class<T> responseType) {
        try {
            String id = UUID.randomUUID().toString();
            var future = registry.register(id);

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

        rabbit.convertAndSend(this.queue, request, message -> {
            message.getMessageProperties().setCorrelationId(id);
            message.getMessageProperties().setReplyTo(callbackUrl);
            message.getMessageProperties().setTimestamp(new java.util.Date());
            return message;
        });
    }
}
