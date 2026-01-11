package tr.kontas.splitr.bus.query;

import java.util.UUID;

public class BaseQuery implements Query {
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
