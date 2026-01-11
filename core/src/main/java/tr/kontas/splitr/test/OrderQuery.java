package tr.kontas.splitr.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.bus.query.BaseQuery;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends BaseQuery {
    private String orderId;
}