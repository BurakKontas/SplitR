package tr.kontas.servicea;

import org.springframework.stereotype.Component;
import tr.kontas.splitr.bus.base.BaseCommandHandler;
import tr.kontas.splitr.test.RandomCommand;

import java.util.UUID;

@Component
public class RandomCommandHandler extends BaseCommandHandler<RandomCommand> {
    @Override
    public String handle(RandomCommand payload) {
        return UUID.randomUUID().toString();
    }
}
