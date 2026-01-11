package tr.kontas.splitr.consumer.bus.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.bus.command.Command;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.consumer.bus.CommandHandler;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InMemoryCommandBus implements CommandBus {

    private final List<CommandHandler<?>> handlers;
    private final LruStore store;
    private Map<Class<?>, CommandHandler<?>> handlerMap;

    private void initializeHandlers() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(
                            CommandHandler::type,
                            handler -> handler,
                            (existing, replacement) -> existing // ilk handler'ı kullan
                    ));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T publishSync(Command command, Class<T> responseType, long timeoutMs) {
        initializeHandlers();

        log.info("Executing command: {} in thread: {}",
                command.getClass().getSimpleName(),
                Thread.currentThread().getName());

        CommandHandler<Command> handler = (CommandHandler<Command>) handlerMap.get(command.getClass());

        if (handler == null) {
            log.warn("No handler found for command type: {}", command.getClass().getName());
            return null;
        }

        // idempotency control
        if(store.contains(command.getIdempotencyKey())) {
            log.info("Command with idempotency key {} has already been processed. Returning cached result.", command.getIdempotencyKey());
            return (T) store.get(command.getIdempotencyKey());
        }

        try {
            T result = (T) handler.handle(command);

            store.put(command.getIdempotencyKey(), result);

            if (result == null) {
                return null;
            }

            if (responseType.isInstance(result)) {
                return responseType.cast(result);
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing command: {}", command.getClass().getSimpleName(), e);
            throw new RuntimeException("Command execution failed", e);
        }
    }

    @Override
    public <T> T publishSync(Command command, Class<T> responseType) {
        return publishSync(command, responseType, Long.MAX_VALUE);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Command command, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> publishSync(command, responseType));
    }

    @Override
    public void publish(Command command) {
        publishSync(command, Void.class);
    }

    @Override
    public void publish(Command command, long timeoutMs) {
        publishSync(command, Void.class, timeoutMs);
    }
}