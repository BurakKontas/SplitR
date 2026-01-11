package tr.kontas.splitr.kafka.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.consumer.autoconfigure.InMemoryBusAutoConfigure;
import tr.kontas.splitr.consumer.bus.CommandHandler;
import tr.kontas.splitr.consumer.bus.EventHandler;
import tr.kontas.splitr.consumer.bus.QueryHandler;
import tr.kontas.splitr.consumer.dispatcher.CommandDispatcher;
import tr.kontas.splitr.consumer.dispatcher.EventDispatcher;
import tr.kontas.splitr.consumer.dispatcher.QueryDispatcher;
import tr.kontas.splitr.consumer.store.IdempotencyStore;
import tr.kontas.splitr.consumer.store.LruStore;
import tr.kontas.splitr.kafka.listener.CommandKafkaListener;
import tr.kontas.splitr.kafka.listener.EventKafkaListener;
import tr.kontas.splitr.kafka.listener.QueryKafkaListener;

import java.util.List;

@AutoConfigureAfter(InMemoryBusAutoConfigure.class)
@Configuration
@EnableKafka
@Slf4j
@ConditionalOnBooleanProperty(name = "splitr.kafka.consumer.enabled")
public class KafkaConsumerAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public QueryKafkaListener queryKafkaListener(QueryDispatcher dispatcher) {
        return new QueryKafkaListener(dispatcher);
    }

    @Bean
    public QueryDispatcher queryDispatcher(
            List<QueryHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing QueryDispatcher");
        return new QueryDispatcher(handlers, store, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandKafkaListener commandKafkaListener(CommandDispatcher dispatcher) {
        return new CommandKafkaListener(dispatcher);
    }

    @Bean
    public CommandDispatcher commandDispatcher(
            List<CommandHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing CommandDispatcher");
        return new CommandDispatcher(handlers, store, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventKafkaListener eventKafkaListener(EventDispatcher dispatcher) {
        return new EventKafkaListener(dispatcher);
    }

    @Bean
    public EventDispatcher eventDispatcher(
            List<EventHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing EventDispatcher");
        return new EventDispatcher(handlers, store, mapper);
    }
}