package tr.kontas.splitr.consumer;


/**
 * The fundamental contract for handling bus messages (commands or queries) within the Splitr ecosystem.
 * <p>
 * Implementations of this interface are intended to be registered as Spring Beans and discovered
 * by the {@code CommandDispatcher} and {@code QueryDispatcher} at runtime to route incoming
 * Kafka message payloads to the appropriate business logic based on the handler type.
 * </p>
 *
 * @param <T> The type of the bus payload this handler supports. Must be a serializable class.
 *
 * @author BurakKontas
 * @version 1.0.0
 */
public interface BusHandler<T> {

    /**
     * Identifies the specific class type that this handler is capable of processing.
     * <p>
     * This is used by the dispatching mechanism to map incoming JSON payloads to the
     * correct handler implementation at runtime.
     * </p>
     *
     * @return The {@link Class} object representing the supported bus payload type.
     */
    Class<T> type();

    /**
     * Processes the given bus payload (command or query) and returns the resulting data.
     * <p>
     * The returned object will typically be serialized into a response message and
     * sent back to the original requester via the configured callback mechanism.
     * </p>
     *
     * @param payload The bus payload object containing request parameters.
     * @return The result of the execution (Response DTO or raw result).
     * @throws InterruptedException If the processing thread is interrupted during execution.
     */
    Object handle(T payload) throws InterruptedException;
}
