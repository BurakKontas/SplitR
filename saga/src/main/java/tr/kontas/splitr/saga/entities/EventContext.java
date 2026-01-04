package tr.kontas.splitr.saga.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tr.kontas.splitr.saga.entities.abstracts.BaseSaga;

@Getter
@Setter
public class EventContext<TEvent> {
    private final TEvent data;
    private Object instance;
    private boolean cancelled = false;
    private final List<Object> publishedMessages = new ArrayList<>();
    private final List<ScheduledMessage> scheduledMessages = new ArrayList<>();
    private BaseSaga<?> saga;

    public EventContext(TEvent data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance() {
        return (T) instance;
    }

    // Publish message (simulated)
    public <T> CompletableFuture<Void> publish(T message) {
        publishedMessages.add(message);
        System.out.println("Published: " + message.getClass().getSimpleName());
        return CompletableFuture.completedFuture(null);
    }

    // Schedule message (simulated)
    public <T> CompletableFuture<Void> schedule(T message, long delayMillis) {
        scheduledMessages.add(new ScheduledMessage(message, delayMillis));
        System.out.println("Scheduled: " + message.getClass().getSimpleName() +
                " in " + delayMillis + "ms");
        return CompletableFuture.completedFuture(null);
    }

    public record ScheduledMessage(Object message, long delayMillis) {}
}