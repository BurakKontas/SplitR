package tr.kontas.splitr.bus.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public interface IdempotencyProtected {
    @JsonIgnore
    String getIdempotencyKey();
    @JsonIgnore
    void setIdempotencyKey(String idempotencyKey);
}
