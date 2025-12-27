package tr.kontas.splitr.consumer;

/**
 * The fundamental interface for handling specific types of queries within the Splitr ecosystem.
 * <p>
 * Implementations of this interface are intended to be registered as Spring Beans.
 * The {@code QueryDispatcher} uses these handlers to route incoming Kafka messages
 * to the appropriate business logic based on the class type.
 * </p>
 *
 * @param <T> The type of the query this handler supports. Usually and must be a serializable class.
 * @author BurakKontas
 * @version 1.0.0
 */
public interface QueryHandler<T> {

    /**
     * Identifies the specific class type of the query that this handler is capable of processing.
     * <p>
     * This is used by the dispatching mechanism to map incoming JSON payloads to the
     * correct handler implementation at runtime.
     * </p>
     *
     * @return The {@link Class} object representing the supported query type.
     */
    Class<T> type();

    /**
     * Processes the given query and returns the resulting data.
     * <p>
     * The returned object will typically be serialized back into a response message
     * and sent to the original requester via the configured callback mechanism.
     * </p>
     *
     * @param query The query object containing the request parameters.
     * @return The result of the query execution (Response DTO).
     * @throws InterruptedException If the processing thread is interrupted during execution.
     */
    Object handle(T query) throws InterruptedException;
}