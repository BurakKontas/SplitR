package tr.kontas.splitr.consumer.dispatcher;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tr.kontas.splitr.consumer.bus.CommandHandler;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.dto.CommandRequest;
import tr.kontas.splitr.dto.CommandResponse;

import java.util.List;

/**
 * Dispatches incoming command requests from listener/consumer to the appropriate {@link CommandHandler}
 * implementation registered in the Spring context.
 *
 * @author BurakKontas
 * @version 1.0.0
 */
@Slf4j
public class CommandDispatcher extends BaseDispatcher<CommandRequest, CommandResponse, CommandHandler<?>> {

    public CommandDispatcher(List<CommandHandler<?>> list, IdempotencyStore store, ObjectMapper mapper) {
        super(list, store, mapper);
    }

    /**
     * Creates a typed {@link CommandResponse} instance correlated to the dispatched command.
     *
     * @param id            Correlation/command ID
     * @param payloadJson   Serialized handler result
     * @return a new {@link CommandResponse} instance
     */
    @Override
    protected CommandResponse createResponse(String id, String payloadJson) {
        return new CommandResponse(id, payloadJson);
    }
}