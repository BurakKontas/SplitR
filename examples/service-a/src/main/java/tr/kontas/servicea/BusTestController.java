package tr.kontas.servicea;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.bus.domainevent.DomainEventBus;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.test.*;

import java.util.UUID;

@RestController
@RequestMapping("/test-bus")
@RequiredArgsConstructor
@Slf4j
public class BusTestController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;
    private final DomainEventBus domainEventBus;

    @GetMapping("/query-sync")
    public String querySync() {
        var query = new OrderQuery("ORD-100");
        return queryBus.publishSync(query, String.class);
    }

    @GetMapping("/query-async")
    public String queryAsync() {
        var query = new OrderQuery("ORD-100");
        queryBus.publishAsync(query, String.class)
                .thenAccept(res -> System.out.println("Async Query Result: " + res));
        return "Async query triggered";
    }

    @GetMapping("/command-sync-timeout")
    public String commandSyncWithTimeout() {
        var command = new CreateOrderCommand("Laptop", 1);
        return commandBus.publishSync(command, String.class, 10);
    }

    @GetMapping("/command-sync-default")
    public String commandSyncDefault() {
        var command = new CreateOrderCommand("Laptop", 1);
        return commandBus.publishSync(command, String.class);
    }

    @GetMapping("/command-sync-idempotency")
    public String commandSyncDefaultIdempotency() {
        var command = new RandomCommand();
        var resp1 = commandBus.publishSync(command, String.class);
        var resp2 = commandBus.publishSync(command, String.class);

        if(resp1.equals(resp2)) {
            return "Idempotent command executed once, second call returned same result: " + resp1;
        }

        throw new RuntimeException("Idempotent command returned different results!");
    }

    @GetMapping("/command-async")
    public String commandAsync() {
        var command = new CreateOrderCommand("Laptop", 1);
        commandBus.publishAsync(command, String.class)
                .thenAccept(res -> System.out.println("Unused Async Command Result: " + res));
        return "Unused async command triggered";
    }

    @GetMapping("/command-fire")
    public String commandFire() {
        var command = new CreateOrderCommand("Laptop", 1);
        commandBus.publish(command);
        return "Fire-and-forget command triggered";
    }

    @GetMapping("/command-fire-timeout")
    public String commandFireWithTimeout() {
        var command = new CreateOrderCommand("Laptop", 1);
        commandBus.publish(command, 10);
        return "Fire-and-forget with timeout triggered";
    }

    @GetMapping("/event-publish")
    public String publishEvent() {
        var event = new OrderProcessedEvent("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        log.info("Publishing distributed event: {}", event);
        eventBus.publish(event);
        return "Distributed event (OrderProcessedEvent) published to Kafka";
    }

    @GetMapping("/domain-event-arise")
    public String ariseDomainEvent() {
        var domainEvent = new OrderDomainEvent("DOM-ID-" + UUID.randomUUID().toString().substring(0, 5));
        log.info("Shadow Monarch commands: ARISE! Event ID: {}", domainEvent.getId());

        domainEventBus.arise(domainEvent);

        return "Domain event (OrderDomainEvent) arose in memory!";
    }
}

