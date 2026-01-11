package tr.kontas.splitr.bus.command;

import java.util.UUID;

public abstract class BaseCommand implements Command {
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
