package tr.kontas.splitr.consumer.bus.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.bus.query.Query;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.consumer.bus.QueryHandler;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InMemoryQueryBus implements QueryBus {

    private final List<QueryHandler<?>> handlers;
    private final LruStore store;
    private Map<Class<?>, QueryHandler<?>> handlerMap;

    private void initializeHandlers() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(
                            QueryHandler::type,
                            handler -> handler,
                            (existing, replacement) -> existing
                    ));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T publishSync(Query query, Class<T> responseType, long timeoutMs) {
        initializeHandlers();

        log.info("Executing query: {} in thread: {}",
                query.getClass().getSimpleName(),
                Thread.currentThread().getName());

        QueryHandler<Query> handler = (QueryHandler<Query>) handlerMap.get(query.getClass());

        if (handler == null) {
            log.warn("No handler found for query type: {}", query.getClass().getName());
            return null;
        }

        if(store.contains(query.getIdempotencyKey())) {
            log.info("Query with idempotency key {} has already been processed. Returning cached result.", query.getIdempotencyKey());
            return (T) store.get(query.getIdempotencyKey());
        }

        try {
            T result = (T) handler.handle(query);

            store.put(query.getIdempotencyKey(), result);

            if (result == null) {
                return null;
            }

            if (responseType.isInstance(result)) {
                return responseType.cast(result);
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing query: {}", query.getClass().getSimpleName(), e);
            throw new RuntimeException("Query execution failed", e);
        }
    }

    @Override
    public <T> T publishSync(Query query, Class<T> responseType) {
        return publishSync(query, responseType, Long.MAX_VALUE);
    }

    @Override
    public <T> CompletableFuture<T> publishAsync(Query query, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> publishSync(query, responseType));
    }
}
