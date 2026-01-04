package tr.kontas.splitr.saga.exceptions;

import lombok.Getter;

@Getter
public class HandlerNotFoundException extends IllegalStateException {
    private final String state;
    private final String eventType;

    public HandlerNotFoundException(String eventType, String state) {
        super(String.format("Handler not found for event '%s' in state '%s'", eventType, state));
        this.state = state;
        this.eventType = eventType;
    }
}
