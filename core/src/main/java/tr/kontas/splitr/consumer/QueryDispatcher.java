package tr.kontas.splitr.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.dto.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class QueryDispatcher {

    private final Map<Class<?>, QueryHandler<?>> handlers;
    private final IdempotencyStore store;
    private final ObjectMapper mapper;
    private final RestTemplate rest = new RestTemplate();

    public QueryDispatcher(List<QueryHandler<?>> list,
                           IdempotencyStore store,
                           ObjectMapper mapper) {
        handlers = list.stream().collect(
                java.util.stream.Collectors.toMap(QueryHandler::type, h->h)
        );

        String joinedNames = handlers.keySet().stream()
                .map(Class::getName)
                .collect(Collectors.joining(", "));

        log.atInfo().log("Handlers: " + joinedNames);
        this.store = store;
        this.mapper = mapper;
    }

    public void dispatch(QueryRequest r) throws Exception {
        log.atInfo().log("Working on: " + r.getId());
        long deadline = r.getSentAtEpochMs() + r.getTimeoutMs();
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) return;

        if (store.contains(r.getId())) {
            rest.postForEntity(r.getCallbackUrl(), store.get(r.getId()), Void.class);
            return;
        }

        Class<?> type = Class.forName(r.getType());
        QueryHandler handler = handlers.get(type);
        Object query = mapper.readValue(r.getPayload(), type);

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                Object result = handler.handle(query);
                QueryResponse resp =
                        new QueryResponse(r.getId(), mapper.writeValueAsString(result));
                store.put(r.getId(), resp);
                rest.postForEntity(r.getCallbackUrl(), resp, Void.class);
            } catch (RuntimeException | JsonProcessingException | InterruptedException e) {
                throw new RuntimeException(e); // TODO: fix later (maybe ?)
            }
        });

        try {
            f.get(remaining, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
        } finally {
            ex.shutdownNow();
        }
    }
}
