package tr.kontas.splitr.consumer.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tr.kontas.splitr.bus.domainevent.DomainEventBus;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.consumer.bus.DomainEventHandler;
import tr.kontas.splitr.consumer.dispatcher.DomainEventDispatcher;
import tr.kontas.splitr.consumer.domainevent.InMemoryEventBus;
import tr.kontas.splitr.consumer.store.LruStore;

import java.util.List;

@Configuration
public class DomainEventAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public DomainEventDispatcher domainEventDispatcher(
            List<DomainEventHandler<?>> handlers,
            LruStore store
    ) {
        return new DomainEventDispatcher(handlers, store);
    }

    @Bean
    public DomainEventBus domainEventBus(
            DomainEventDispatcher domainEventDispatcher
    ) {
        return new InMemoryEventBus(domainEventDispatcher);
    }
}