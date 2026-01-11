package tr.kontas.splitr.consumer.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import tr.kontas.splitr.consumer.bus.BusHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.dto.CommandRequest;
import tr.kontas.splitr.dto.EventRequest;
import tr.kontas.splitr.dto.QueryRequest;
import tr.kontas.splitr.dto.base.BaseRequest;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseDispatcher<TReq extends BaseRequest, TResp extends BaseResponse, THandler extends BusHandler<?>> {

    protected final Map<Class<?>, List<THandler>> handlers;
    protected final IdempotencyStore store;
    protected final ObjectMapper mapper;
    protected static final RestTemplate rest = new RestTemplate();

    protected BaseDispatcher(List<THandler> list, IdempotencyStore store, ObjectMapper mapper) {
        this.handlers = list.stream().collect(Collectors.groupingBy(BusHandler::type));
        this.store = store;
        this.mapper = mapper;
    }

    public void dispatch(TReq r) throws Exception {
        log.atInfo().log("Working on: " + r.getId());

        long deadline = r.getSentAtEpochMs() + r.getTimeoutMs();
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) return;

        if (store.contains(r.getId())) {
            triggerWebhook(r, store.get(r.getId()));
            return;
        }

        Class<?> type = Class.forName(r.getType());
        List<THandler> typeHandlers = handlers.get(type);

        if (typeHandlers == null || typeHandlers.isEmpty()) {
            log.warn("No handler found for type: {}", type);
            return;
        }

        Object payloadObj = mapper.readValue(r.getPayload(), type);
        boolean isEvent = r instanceof EventRequest;

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                if (isEvent) {
                    // EVENT ise: Tüm handler'ları dön
                    for (THandler h : typeHandlers) {
                        ((BusHandler<Object>) h).handle(payloadObj);
                    }
                    // Eventlerde genellikle bir "sonuç" (return value) beklenmez
                    // veya boş dönülür. Webhook tetiklenmeyeceği için burası opsiyoneldir.
                } else {
                    // COMMAND ise: Sadece ilk handler'ı çalıştır ve sonucu dön
                    Object result = ((BusHandler<Object>) typeHandlers.getFirst()).handle(payloadObj);
                    TResp resp = createResponse(r.getId(), mapper.writeValueAsString(result));
                    store.put(r.getId(), resp);
                    triggerWebhook(r, resp);
                }
            } catch (Exception e) {
                log.error("Error while processing handlers", e);
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

    private void triggerWebhook(TReq r, Object resp) {
        if (r instanceof EventRequest) {
            return;
        }

        String typePath = switch (r) {
            case QueryRequest q -> "query";
            case CommandRequest c -> "command";
            default -> "unknown";
        };

        // Eğer callbackUrl "http://service-a/callback/%s" şeklinde geliyorsa doldurur
        String finalUrl = r.getCallbackUrl();
        if (finalUrl.contains("%s")) {
            finalUrl = String.format(finalUrl, typePath);
        }

        try {
            if(!finalUrl.isBlank())
                rest.postForEntity(finalUrl, resp, Void.class);
        } catch (Exception e) {
            log.error("Failed to trigger webhook for ID: {}", r.getId(), e);
        }
    }

    protected abstract TResp createResponse(String id, String payloadJson);
}
