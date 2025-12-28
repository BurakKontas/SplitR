package tr.kontas.splitr.consumer.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.consumer.bus.EventHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.dto.EventRequest;
import tr.kontas.splitr.dto.base.BaseResponse;

import java.util.List;

/**
 * Dispatches incoming query requests from listener/consumer to the appropriate {@link EventHandler}
 * implementation registered in the Spring context.
 *
 * @author BurakKontas
 * @version 1.0.0
 */
@Slf4j
public class EventDispatcher extends BaseDispatcher<EventRequest, BaseResponse, EventHandler<?>> {

    public EventDispatcher(List<EventHandler<?>> list, IdempotencyStore store, ObjectMapper mapper) {
        super(list, store, mapper);
    }

    /**
     * Creates a typed {@link BaseResponse} instance correlated to the dispatched query.
     *
     * @param id          Correlation/query ID
     * @param payloadJson Serialized handler result
     * @return a new {@link BaseResponse} instance
     */
    @Override
    protected BaseResponse createResponse(String id, String payloadJson) {
        return new BaseResponse("", ""); // no response
    }
}
