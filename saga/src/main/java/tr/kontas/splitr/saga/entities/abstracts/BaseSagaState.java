package tr.kontas.splitr.saga.entities.abstracts;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseSagaState {

    public BaseSagaState(String name) {
        this.name = name;
    }

    /** Unique correlation ID for this saga instance */
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id = UUID.randomUUID().toString();

    @Setter(AccessLevel.PROTECTED)
    private String correlationId = UUID.randomUUID().toString();

    /** Creation timestamp */
    @Column(name = "created_at", nullable = false, updatable = false)
    private long createdAt = System.currentTimeMillis();

    /** Last updated timestamp */
    @Column(name = "updated_at", nullable = false)
    private long updatedAt = System.currentTimeMillis();

    /** Current status of the saga */
    @Column(name = "status", nullable = false)
    private String currentState;

    private boolean cancelled = false;
    private boolean completed = false;

    /** Optional saga name or type default is class name */
    private String name;

    @Version
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
