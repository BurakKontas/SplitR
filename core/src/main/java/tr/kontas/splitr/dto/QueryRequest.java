package tr.kontas.splitr.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tr.kontas.splitr.dto.base.BaseRequest;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryRequest extends BaseRequest {
    public QueryRequest(
        String id,
        String type,
        String payload,
        String callbackUrl,
        boolean isSync,
        long sentAtEpochMs,
        long timeoutMs
    ) {
        super(id, type, payload, callbackUrl, isSync, sentAtEpochMs, timeoutMs, 0);
    }
}
