package tr.kontas.splitr.bus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import tr.kontas.splitr.dto.QueryRequest;

@Configuration
@ConditionalOnProperty(name = "splitr.publisher.enabled", havingValue = "true")
public class QueryBusAutoConfig {

    @Bean
    public SyncRegistry syncRegistry() {
        return new SyncRegistry();
    }

    @Bean
    public QueryCallbackController queryCallbackController(SyncRegistry registry) {
        return new QueryCallbackController(registry);
    }

    @Bean
    public QueryBus queryBus(
            KafkaTemplate<String, QueryRequest> kafka,
            SyncRegistry registry,
            ObjectMapper mapper,
            @Value("${splitr.callback-url}") String url,
            @Value("${splitr.bus.kafka.topic:tr.kontas.splitr.query.topic}") String queryTopic,
            @Value("${splitr.bus.default-timeout:10}") int defaultTimeout
        ) {
        return new KafkaQueryBus(queryTopic, kafka, registry, mapper, url, defaultTimeout);
    }
}
