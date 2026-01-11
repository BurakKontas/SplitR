package tr.kontas.splitr.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.bus.domainevent.DomainEvent;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDomainEvent implements DomainEvent {
    private String id;
}
