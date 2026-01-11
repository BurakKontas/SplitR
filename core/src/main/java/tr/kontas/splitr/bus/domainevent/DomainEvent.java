package tr.kontas.splitr.bus.domainevent;

import java.util.UUID;

public interface DomainEvent {
    default String getIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
