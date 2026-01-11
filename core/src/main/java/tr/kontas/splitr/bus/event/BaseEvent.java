package tr.kontas.splitr.bus.event;

import java.util.UUID;

public class BaseEvent implements Event {
    private String idempotencyKey = UUID.randomUUID().toString();

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    @Override
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
