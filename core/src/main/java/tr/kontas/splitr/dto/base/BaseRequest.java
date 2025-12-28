package tr.kontas.splitr.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequest {
    private String id;
    private String type;
    private String payload;
    private String callbackUrl;
    private boolean isSync;
    private long sentAtEpochMs;
    private long timeoutMs;
    private int retryCount = 0;
}

