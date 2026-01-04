package tr.kontas.splitr.saga.entities;

import lombok.Getter;

import java.util.function.Function;

@Getter
public class Event<T> {
    private final Class<T> eventType;
    private Function<T, String> correlationFunction;

    public Event(Class<T> eventType) {
        this.eventType = eventType;
    }

    public void correlateById(Function<T, String> correlateFunction) {
        this.correlationFunction = correlateFunction;
    }
}
