package tr.kontas.splitr.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.bus.command.BaseCommand;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateOrderCommand extends BaseCommand {
    private String productName;
    private int quantity;
}
