package tr.kontas.splitr.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.dto.QueryRequest;

@RequiredArgsConstructor
@Slf4j
public class QueryKafkaListener {

    private final QueryDispatcher dispatcher;

    @KafkaListener(
            topics = "${splitr.bus.kafka.topic:tr.kontas.splitr.query.topic}",
            groupId = "${splitr.bus.kafka.consumer:tr.kontas.splitr.query.consumer}"
    )
    public void listen(QueryRequest r) throws Exception {
        log.atInfo().log("Dispatching: " + r.queryId());
        dispatcher.dispatch(r);
    }
}