package tr.kontas.splitr.kafka.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.bus.CommandBus;
import tr.kontas.splitr.bus.SyncRegistry;
import tr.kontas.splitr.dto.CommandRequest;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class KafkaCommandBus extends AbstractKafkaBus<CommandRequest> implements CommandBus {

    public KafkaCommandBus(String commandTopic, KafkaTemplate<String, CommandRequest> kafka,
                           SyncRegistry registry, ObjectMapper mapper,
                           String callbackUrl, int defaultTimeout) {
        super(commandTopic, kafka, registry, mapper, callbackUrl, defaultTimeout);
    }

    @Override
    protected CommandRequest createRequest(String id, String typeName, String payload, boolean isSync, long now, long timeout) {
        return new CommandRequest(id, typeName, payload, callbackUrl, isSync, now, timeout);
    }

    @Override
    public <T> T publishSync(Object command, Class<T> responseType, long timeoutMs) {
        return executeSync(command, responseType, timeoutMs);
    }

    @Override
    public <T> T publishSync(Object command, Class<T> responseType) {
        return publishSync(command, responseType, defaultTimeout);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Object command, Class<T> responseType) {
        return executeAsync(command, responseType, true);
    }

    @Override
    public void publish(Object command) {
        publish(command, Long.MAX_VALUE);
    }

    @Override
    public void publish(Object command, long timeoutMs) {
        try {
            sendInternal(UUID.randomUUID().toString(), command, false, timeoutMs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}