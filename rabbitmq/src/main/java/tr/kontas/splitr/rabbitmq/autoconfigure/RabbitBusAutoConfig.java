package tr.kontas.splitr.rabbitmq.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tr.kontas.splitr.bus.command.CommandBus;
import tr.kontas.splitr.bus.command.CommandCallbackController;
import tr.kontas.splitr.bus.event.EventBus;
import tr.kontas.splitr.bus.query.QueryBus;
import tr.kontas.splitr.bus.query.QueryCallbackController;
import tr.kontas.splitr.bus.registry.SyncRegistry;
import tr.kontas.splitr.rabbitmq.bus.RabbitCommandBus;
import tr.kontas.splitr.rabbitmq.bus.RabbitEventBus;
import tr.kontas.splitr.rabbitmq.bus.RabbitQueryBus;

@Configuration
@ConditionalOnProperty(name = "splitr.publisher.enabled", havingValue = "true")
public class RabbitBusAutoConfig {

    @Bean
    public Jackson2JsonMessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean("splitrRabbitTemplate")
    @Primary
    public RabbitTemplate splitrRabbitTemplate(ConnectionFactory connectionFactory,
                                               Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public SyncRegistry syncRegistry() {
        return new SyncRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryCallbackController queryCallbackController(SyncRegistry registry) {
        return new QueryCallbackController(registry);
    }

    @Bean
    public QueryBus queryBus(
            RabbitTemplate rabbit,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.rabbit.query.queue:tr.kontas.splitr.query.queue}") String queue,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
    ) {
        return new RabbitQueryBus(queue, rabbit, registry, mapper, url, defaultTimeout);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandCallbackController commandCallbackController(SyncRegistry registry) {
        return new CommandCallbackController(registry);
    }

    @Bean
    public CommandBus commandBus(
            RabbitTemplate rabbit,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.rabbit.command.queue:tr.kontas.splitr.command.queue}") String queue,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
    ) {
        return new RabbitCommandBus(queue, rabbit, registry, mapper, url, defaultTimeout);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus(
            RabbitTemplate rabbit,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.rabbit.event.queue:tr.kontas.splitr.event.queue}") String queue,
            @Value("${splitr.bus.default-timeout:10000}") int defaultTimeout
    ) {
        return new RabbitEventBus(queue, rabbit, registry, mapper, url, defaultTimeout);
    }
}

