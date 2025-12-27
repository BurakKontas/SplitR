package tr.kontas.splitr.dto;

public record QueryRequest(
        String queryId,
        String queryType,
        String payload,
        String callbackUrl,
        long sentAtEpochMs,
        long timeoutMs
) {}
