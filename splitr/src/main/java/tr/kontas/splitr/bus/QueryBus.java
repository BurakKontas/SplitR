package tr.kontas.splitr.bus;

/**
 * Provides a synchronous contract for dispatching queries across the distributed system.
 * <p>
 * This interface abstracts the underlying transport mechanism, allowing callers to
 * execute queries and wait for a typed response in a blocking manner.
 * </p>
 *
 * @author BurakKontas
 * @version 1.0.0
 */
public interface QueryBus {

    /**
     * Dispatches a query and waits for a response within a specified timeout.
     * <p>
     * This method bypasses the default configuration and uses the provided
     * {@code timeoutMs} for this specific execution.
     * </p>
     *
     * @param <T>          The expected type of the response.
     * @param query        The query payload object to be processed.
     * @param responseType The class of the expected response for deserialization.
     * @param timeoutMs    The maximum time to wait for the response in milliseconds.
     * @return             The processed result of the query.
     * @throws RuntimeException if the query fails, serialization fails, or the timeout is exceeded.
     */
    <T> T querySync(Object query, Class<T> responseType, long timeoutMs);

    /**
     * Dispatches a query and waits for a response using the default system timeout.
     * <p>
     * The default timeout value is globally configured via the property
     * {@code splitr.bus.default-timeout} (Defaulting to 10ms if not specified).
     * </p>
     *
     * @param <T>          The expected type of the response.
     * @param query        The query payload object to be processed.
     * @param responseType The class of the expected response for deserialization.
     * @return             The processed result of the query.
     * @throws RuntimeException if the query fails or the default timeout is exceeded.
     */
    <T> T querySync(Object query, Class<T> responseType);
}