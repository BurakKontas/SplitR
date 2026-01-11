package tr.kontas.splitr.rabbitmq.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
import tr.kontas.splitr.rabbitmq.listener.CommandRabbitListener;
import tr.kontas.splitr.rabbitmq.listener.EventRabbitListener;
import tr.kontas.splitr.rabbitmq.listener.QueryRabbitListener;

import java.util.List;

@AutoConfigureAfter(InMemoryBusAutoConfigure.class)
@Configuration
@EnableRabbit
@Slf4j
@ConditionalOnBooleanProperty(name = "splitr.rabbit.consumer.enabled")
public class RabbitConsumerAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public QueryRabbitListener queryRabbitListener(QueryDispatcher dispatcher) {
        return new QueryRabbitListener(dispatcher);
    }

    @Bean(name = "rabbitQueryDispatcher")
    @Primary
    public QueryDispatcher queryDispatcher(
            List<QueryHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing QueryDispatcher (RabbitMQ)");
        return new QueryDispatcher(handlers, store, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandRabbitListener commandRabbitListener(CommandDispatcher dispatcher) {
        return new CommandRabbitListener(dispatcher);
    }

    @Bean("rabbitDispatcher")
    @Primary
    public CommandDispatcher commandDispatcher(
            List<CommandHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing CommandDispatcher (RabbitMQ)");
        return new CommandDispatcher(handlers, store, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventRabbitListener eventRabbitListener(EventDispatcher dispatcher) {
        return new EventRabbitListener(dispatcher);
    }

    @Bean
    public EventDispatcher eventDispatcher(
            List<EventHandler<?>> handlers,
            IdempotencyStore store,
            ObjectMapper mapper) {
        log.atInfo().log("Initializing EventDispatcher (RabbitMQ)");
        return new EventDispatcher(handlers, store, mapper);
    }

    @Bean("splitrSimpleRabbitListenerContainerFactory")
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter); // JSON dönüşümünü açıyoruz
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
