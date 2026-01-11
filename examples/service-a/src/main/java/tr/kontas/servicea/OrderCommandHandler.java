package tr.kontas.servicea;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tr.kontas.splitr.bus.base.BaseCommandHandler;
import tr.kontas.splitr.test.CreateOrderCommand;

@Component
@Slf4j
public class OrderCommandHandler extends BaseCommandHandler<CreateOrderCommand> {
    @Override
    public String handle(CreateOrderCommand cmd) {
        log.atInfo().log("Command executed for: %s %d".formatted(cmd.getProductName(), cmd.getQuantity()));
        return "SUCCESS";
    }
}
