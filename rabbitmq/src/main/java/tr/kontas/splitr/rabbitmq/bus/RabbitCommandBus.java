package tr.kontas.splitr.rabbitmq.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tr.kontas.splitr.bus.command.Command;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.dto.CommandRequest;
import tr.kontas.splitr.rabbitmq.bus.base.AbstractRabbitBus;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RabbitCommandBus extends AbstractRabbitBus<CommandRequest> implements CommandBus {

    public RabbitCommandBus(String queue, RabbitTemplate rabbit,
                            SyncRegistry registry, ObjectMapper mapper,
                            String callbackUrl, int defaultTimeout) {
        super(queue, rabbit, registry, mapper, callbackUrl, defaultTimeout);
    }

    @Override
    protected CommandRequest createRequest(String id, String typeName, String payload,
                                           boolean isSync, long now, long timeout) {
        return new CommandRequest(id, typeName, payload, callbackUrl, isSync, now, timeout);
    }

    @Override
    public <T> T publishSync(Command command, Class<T> responseType, long timeoutMs) {
        return executeSync(command, responseType, timeoutMs);
    }

    @Override
    public <T> T publishSync(Command command, Class<T> responseType) {
        return publishSync(command, responseType, defaultTimeout);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Command command, Class<T> responseType) {
        return executeAsync(command, responseType);
    }

    @Override
    public void publish(Command command) {
        publish(command, Long.MAX_VALUE);
    }

    @Override
    public void publish(Command command, long timeoutMs) {
        try {
            sendInternal(command, false, timeoutMs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
