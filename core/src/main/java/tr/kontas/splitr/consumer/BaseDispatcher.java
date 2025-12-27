package tr.kontas.splitr.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import tr.kontas.splitr.dto.base.BaseRequest;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseDispatcher<TReq extends BaseRequest, TResp extends BaseResponse, THandler extends BusHandler<?>> {

    protected final Map<Class<?>, THandler> handlers;
    protected final IdempotencyStore store;
    protected final ObjectMapper mapper;
    protected final RestTemplate rest = new RestTemplate();

    protected BaseDispatcher(List<THandler> list, IdempotencyStore store, ObjectMapper mapper) {
        this.handlers = list.stream().collect(Collectors.toMap(BusHandler::type, h -> h));
        this.store = store;
        this.mapper = mapper;

        log.atInfo().log("Handlers: " + handlers.keySet().stream()
                .map(Class::getName)
                .collect(Collectors.joining(", ")));
    }

    public void dispatch(TReq r) throws Exception {
        log.atInfo().log("Working on: " + r.getId());

        long deadline = r.getSentAtEpochMs() + r.getTimeoutMs();
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) return;

        if (store.contains(r.getId())) {
            rest.postForEntity(r.getCallbackUrl(), store.get(r.getId()), Void.class);
            return;
        }

        Class<?> type = Class.forName(r.getType());
        THandler handler = handlers.get(type);
        Object payloadObj = mapper.readValue(r.getPayload(), type);

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                Object result = ((BusHandler<Object>) handler).handle(payloadObj);
                TResp resp = createResponse(r.getId(), mapper.writeValueAsString(result));
                store.put(r.getId(), resp);
                rest.postForEntity(r.getCallbackUrl(), resp, Void.class);
            } catch (RuntimeException | JsonProcessingException | InterruptedException e) {
                throw new RuntimeException(e);
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

    protected abstract TResp createResponse(String id, String payloadJson);
}
