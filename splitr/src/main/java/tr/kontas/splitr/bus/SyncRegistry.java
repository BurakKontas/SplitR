package tr.kontas.splitr.bus;

import tr.kontas.splitr.dto.QueryResponse;

import java.util.concurrent.*;

public class SyncRegistry {
    private final ConcurrentHashMap<String, CompletableFuture<QueryResponse>> map = new ConcurrentHashMap<>();
    public CompletableFuture<QueryResponse> register(String id) {
        var f = new CompletableFuture<QueryResponse>();
        map.put(id, f);
        return f;
    }
    public void complete(QueryResponse r) {
        var f = map.remove(r.queryId());
        if (f != null) f.complete(r);
    }
}
