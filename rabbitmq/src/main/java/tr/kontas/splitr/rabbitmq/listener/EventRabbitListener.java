package tr.kontas.splitr.rabbitmq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tr.kontas.splitr.consumer.dispatcher.EventDispatcher;
import tr.kontas.splitr.dto.EventRequest;

@RequiredArgsConstructor
@Slf4j
public class EventRabbitListener {

    private final EventDispatcher dispatcher;

    @RabbitListener(queuesToDeclare = @Queue(name = "${splitr.rabbit.event.queue:tr.kontas.splitr.event.queue}", durable = "true"))
    public void listen(EventRequest r) throws Exception {
        log.atInfo().log("Dispatching event: " + r.getId());
        dispatcher.dispatch(r);
    }
}

