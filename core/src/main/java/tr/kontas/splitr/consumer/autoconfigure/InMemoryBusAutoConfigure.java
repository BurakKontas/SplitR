package tr.kontas.splitr.consumer.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.consumer.bus.CommandHandler;
import tr.kontas.splitr.consumer.bus.EventHandler;
import tr.kontas.splitr.consumer.bus.QueryHandler;
import tr.kontas.splitr.consumer.bus.impl.*;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.List;

@Configuration
public class InMemoryBusAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public SyncRegistry syncRegistry(@Value("${splitr.registry.max-size:10000}") int max, @Value("${splitr.registry.cleanup-interval-ms:10000}") long cleanupIntervalMs) {
        SyncRegistry registry = new SyncRegistry(cleanupIntervalMs, max);

        Runtime.getRuntime().addShutdownHook(new Thread(registry::shutdown));

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public LruStore lruStore(@Value("${splitr.idempotency.max-size:100}") int max, @Value("${splitr.idempotency.ttl-ms:3600000}") long ttlMs) {
        LruStore store = new LruStore(
                max,
                ttlMs
        );

        Runtime.getRuntime().addShutdownHook(new Thread(store::shutdown));

        return store;
    }

    @Bean
    @ConditionalOnBooleanProperty(value = "splitr.inmemory.enabled")
    public CommandBus commandBus(List<CommandHandler<?>> handlers, LruStore store) {
        return new InMemoryCommandBus(handlers, store);
    }

    @Bean
    @ConditionalOnBooleanProperty(value = "splitr.inmemory.enabled")
    public QueryBus queryBus(List<QueryHandler<?>> handlers, LruStore store) {
        return new InMemoryQueryBus(handlers, store);
    }

    @Bean
    @ConditionalOnBooleanProperty(value = "splitr.inmemory.enabled")
    public EventBus eventBus(List<EventHandler<?>> handlers, LruStore store) {
        return new InMemoryEventBus(handlers, store);
    }
}
