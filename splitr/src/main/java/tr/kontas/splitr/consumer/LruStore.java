package tr.kontas.splitr.consumer;

import tr.kontas.splitr.dto.QueryResponse;

import java.util.*;

public class LruStore implements IdempotencyStore {

    private final Map<String, QueryResponse> cache;

    public LruStore(int max) {
        cache = new LinkedHashMap<>(16,0.75f,true) {
            protected boolean removeEldestEntry(Map.Entry<String,QueryResponse> e) {
                return size() > max;
            }
        };
    }
    public boolean contains(String id){ return cache.containsKey(id); }
    public QueryResponse get(String id){ return cache.get(id); }
    public void put(String id, QueryResponse r){ cache.put(id,r); }
}