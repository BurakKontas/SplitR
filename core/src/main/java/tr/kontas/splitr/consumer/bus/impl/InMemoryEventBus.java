package tr.kontas.splitr.consumer.bus.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.bus.event.Event;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.consumer.bus.EventHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InMemoryEventBus implements EventBus {

    private final List<EventHandler<?>> handlers;
    private final LruStore store;
    private Map<Class<?>, List<EventHandler<?>>> handlerMap;

    private void initializeHandlers() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.groupingBy(EventHandler::type));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        initializeHandlers();

        log.info("Publishing event: {} in thread: {}",
                event.getClass().getSimpleName(),
                Thread.currentThread().getName());

        List<EventHandler<?>> eventHandlers = handlerMap.get(event.getClass());

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            log.warn("No handler found for event type: {}", event.getClass().getName());
            return;
        }

        if(store.contains(event.getIdempotencyKey())) {
            log.info("Event with idempotency key {} has already been processed. Skipping.", event.getIdempotencyKey());
            return;
        }

        for (EventHandler<?> handler : eventHandlers) {
            try {
                ((EventHandler<Event>) handler).handle(event);
                log.debug("Event handled successfully by: {}", handler.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Error in event handler: {} for event: {}",
                        handler.getClass().getSimpleName(),
                        event.getClass().getSimpleName(),
                        e);
            }
        }
    }
}