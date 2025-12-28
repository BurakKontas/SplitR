package tr.kontas.splitr.rabbitmq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tr.kontas.splitr.consumer.dispatcher.QueryDispatcher;
import tr.kontas.splitr.dto.QueryRequest;

@RequiredArgsConstructor
@Slf4j
public class QueryRabbitListener {

    private final QueryDispatcher dispatcher;

    @RabbitListener(queuesToDeclare = @Queue(name = "${splitr.rabbit.query.queue:tr.kontas.splitr.query.queue}", durable = "true"))
    public void listen(QueryRequest r) throws Exception {
        log.atInfo().log("Dispatching query: " + r.getId());
        dispatcher.dispatch(r);
    }
}
