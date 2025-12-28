package tr.kontas.splitr.consumer.dispatcher;

import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.consumer.bus.DomainEventHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.dto.DomainEventRequest;

import java.util.List;

@Slf4j
public class DomainEventDispatcher {

    private final List<DomainEventHandler<?>> handlers;
    private final IdempotencyStore store;

    public DomainEventDispatcher(List<DomainEventHandler<?>> handlers, IdempotencyStore store) {
        this.handlers = handlers;
        this.store = store;
    }

    public void dispatch(DomainEventRequest event) {
        String eventId = event.getId();

        // 1. Idempotency Kontrolü: Bu ID daha önce başarıyla işlendi mi?
        if (store.contains(eventId)) {
            log.atInfo().log("Event already processed, skipping: " + eventId);
            return;
        }

        log.atInfo().log("Processing event: " + eventId);

        if (handlers.isEmpty()) {
            return;
        }

        boolean handlerFound = false;

        try {
            // 2. Tüm handler'ları çalıştır
            for (DomainEventHandler<?> h : handlers) {
                if (h.type().equals(event.getPayload().getClass())) {
                    ((DomainEventHandler<Object>) h).handle(event.getPayload());
                    handlerFound = true;
                }
            }

            if (!handlerFound) {
                log.error("No DomainEventHandler found for payload type: {}", event.getPayload().getClass().getName());
            }

            store.put(eventId, null);

        } catch (Exception e) {
            log.error("Error while processing event: {}", eventId, e);
            throw new RuntimeException(e);
        }
    }
}
