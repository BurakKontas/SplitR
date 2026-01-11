package tr.kontas.splitr.kafka.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.bus.command.CommandCallbackController;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.bus.query.QueryCallbackController;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.consumer.autoconfigure.InMemoryBusAutoConfigure;
import tr.kontas.splitr.consumer.store.LruStore;
import tr.kontas.splitr.dto.CommandRequest;
import tr.kontas.splitr.dto.EventRequest;
import tr.kontas.splitr.dto.QueryRequest;
import tr.kontas.splitr.kafka.bus.KafkaCommandBus;
import tr.kontas.splitr.kafka.bus.KafkaEventBus;
import tr.kontas.splitr.kafka.bus.KafkaQueryBus;

@AutoConfigureAfter(InMemoryBusAutoConfigure.class)
@Configuration
@ConditionalOnBooleanProperty(name = "splitr.kafka.publisher.enabled")
public class KafkaBusAutoConfig {

    @Bean("queryKafka")
    @ConditionalOnMissingBean
    public KafkaTemplate<String, QueryRequest> kafkaQueryTemplate(
            ProducerFactory<String, QueryRequest> producerFactory
    ) {
        if (producerFactory == null) {
            throw new RuntimeException("ProducerFactory<String, QueryRequest> bean is null");
        }

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public QueryCallbackController queryCallbackController(SyncRegistry registry) {
        return new QueryCallbackController(registry);
    }

    @Bean("kafkaQueryBus")
    @Primary
    public QueryBus queryBus(
            KafkaTemplate<String, QueryRequest> kafka,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.bus.kafka.query.topic:tr.kontas.splitr.query.topic}") String queryTopic,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
        ) {
        return new KafkaQueryBus(queryTopic, kafka, registry, mapper, url, defaultTimeout);
    }

    @Bean("commandKafka")
    @ConditionalOnMissingBean
    public KafkaTemplate<String, CommandRequest> kafkaCommandTemplate(
            ProducerFactory<String, CommandRequest> producerFactory
    ) {
        if (producerFactory == null) {
            throw new RuntimeException("ProducerFactory<String, CommandRequest> bean is null");
        }

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public CommandCallbackController commandCallbackController(SyncRegistry registry) {
        return new CommandCallbackController(registry);
    }

    @Bean("kafkaCommandBus")
    @Primary
    public CommandBus commandBus(
            KafkaTemplate<String, CommandRequest> kafka,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.bus.kafka.command.topic:tr.kontas.splitr.command.topic}") String queryTopic,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
    ) {
        return new KafkaCommandBus(queryTopic, kafka, registry, mapper, url, defaultTimeout);
    }

    @Bean("eventKafka")
    @ConditionalOnMissingBean
    public KafkaTemplate<String, EventRequest> kafkaEventTemplate(
            ProducerFactory<String, EventRequest> producerFactory
    ) {
        if (producerFactory == null) {
            throw new RuntimeException("ProducerFactory<String, EventRequest> bean is null");
        }

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean("kafkaEventBus")
    @Primary
    public EventBus eventBus(
            KafkaTemplate<String, EventRequest> kafka,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.bus.kafka.event.topic:tr.kontas.splitr.event.topic}") String queryTopic,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
    ) {
        return new KafkaEventBus(queryTopic, kafka, registry, mapper, url, defaultTimeout);
    }
}
