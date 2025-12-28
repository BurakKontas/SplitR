package tr.kontas.splitr.rabbitmq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tr.kontas.splitr.consumer.dispatcher.CommandDispatcher;
import tr.kontas.splitr.dto.CommandRequest;

@RequiredArgsConstructor
@Slf4j
public class CommandRabbitListener {

    private final CommandDispatcher dispatcher;

    @RabbitListener(queuesToDeclare = @Queue(name = "${splitr.rabbit.command.queue:tr.kontas.splitr.command.queue}", durable = "true"))
    public void listen(CommandRequest r) throws Exception {
        log.atInfo().log("Dispatching command: " + r.getId());
        dispatcher.dispatch(r);
    }
}
