package tr.kontas.splitr.consumer.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.consumer.bus.QueryHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.dto.QueryRequest;
import tr.kontas.splitr.dto.QueryResponse;

import java.util.List;

/**
 * Dispatches incoming query requests from listener/consumer to the appropriate {@link QueryHandler}
 * implementation registered in the Spring context.
 *
 * @author BurakKontas
 * @version 1.0.0
 */
@Slf4j
public class QueryDispatcher extends BaseDispatcher<QueryRequest, QueryResponse, QueryHandler<?>> {

    public QueryDispatcher(List<QueryHandler<?>> list, IdempotencyStore store, ObjectMapper mapper) {
        super(list, store, mapper);
    }

    /**
     * Creates a typed {@link QueryResponse} instance correlated to the dispatched query.
     *
     * @param id            Correlation/query ID
     * @param payloadJson   Serialized handler result
     * @return a new {@link QueryResponse} instance
     */
    @Override
    protected QueryResponse createResponse(String id, String payloadJson) {
        return new QueryResponse(id, payloadJson);
    }
}
