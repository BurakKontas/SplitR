package tr.kontas.servicea;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.kontas.splitr.bus.QueryBus;
import tr.kontas.splitr.test.OrderQuery;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final QueryBus bus;

    @GetMapping("/order")
    public String order() {
        String id = UUID.randomUUID().toString();
        System.out.println("Sending: " + id);
        return bus.querySync(new OrderQuery(id), String.class);
    }
}
