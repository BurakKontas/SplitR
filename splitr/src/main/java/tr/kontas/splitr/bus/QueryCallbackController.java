package tr.kontas.splitr.bus;

import org.springframework.web.bind.annotation.*;
import tr.kontas.splitr.dto.QueryResponse;

@RestController
@RequestMapping("/internal/query")
public class QueryCallbackController {

    private final SyncRegistry registry;
    public QueryCallbackController(SyncRegistry registry) { this.registry = registry; }

    @PostMapping("/callback")
    public void callback(@RequestBody QueryResponse r) {
        registry.complete(r);
    }
}
