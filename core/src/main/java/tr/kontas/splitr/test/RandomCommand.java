package tr.kontas.splitr.test;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.bus.command.BaseCommand;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RandomCommand extends BaseCommand {
}
