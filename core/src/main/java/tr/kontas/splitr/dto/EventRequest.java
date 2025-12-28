package tr.kontas.splitr.dto;

import lombok.AllArgsConstructor;
import tr.kontas.splitr.dto.base.BaseRequest;


@AllArgsConstructor
public class EventRequest extends BaseRequest {
    public EventRequest(
            String id,
            String type,
            String payload
    ) {
        super(id, type, payload, "", false, System.currentTimeMillis(), Long.MAX_VALUE, 0);
    }
}
