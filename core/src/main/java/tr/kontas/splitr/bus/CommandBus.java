package tr.kontas.splitr.bus;

import java.util.concurrent.CompletableFuture;

/**
 * Provides a contract for dispatching commands across the distributed system.
 * <p>
 * This interface abstracts the underlying transport mechanism, allowing callers to
 * publish commands in both blocking (sync) and non-blocking (async) manners and receive
 * a typed response correlated to the published command.
 * </p>
 *
 * @author BurakKontas
 * @version 1.0.0
 */
public interface CommandBus {

    /**
     * Publishes a command and waits for a typed response within a specified timeout.
     * <p>
     * This method bypasses the default configuration and uses the provided
     * {@code timeoutMs} for this specific execution.
     * </p>
     *
     * @param <T>          The expected type of the response.
     * @param command      The command payload object to be processed.
     * @param responseType The class of the expected response for deserialization.
     * @param timeoutMs    The maximum time to wait for the response in milliseconds.
     * @return             The processed result of the command.
     * @throws RuntimeException if the command fails, serialization fails, or the timeout is exceeded.
     */
    <T> T publishSync(Object command, Class<T> responseType, long timeoutMs);

    /**
     * Publishes a command and waits for a typed response using the default system timeout.
     * <p>
     * The default timeout value is globally configured via the property
     * {@code splitr.bus.default-timeout} (Defaulting to 10ms if not specified).
     * </p>
     *
     * @param <T>          The expected type of the response.
     * @param command      The command payload object to be processed.
     * @param responseType The class of the expected response for deserialization.
     * @return             The processed result of the command.
     * @throws RuntimeException if the command fails or the default timeout is exceeded.
     */
    <T> T publishSync(Object command, Class<T> responseType);

    /**
     * Publishes a command without blocking and returns a future for the typed response
     * using the default system timeout internally.
     * <p>
     * Timeout is configured by user.
     * </p>
     *
     * @param <T>          The expected type of the response.
     * @param command      The command payload object to be processed.
     * @param responseType The class of the expected response for deserialization.
     * @return             A future holding the result of the command.
     */
    <T> CompletableFuture<T> publishAsync(Object command, Class<T> responseType);

    /**
     * Publishes a command without expecting any response (fire-and-forget).
     *
     * @param command The command payload object to be dispatched.
     */
    void publish(Object command);

    /**
     * Publishes a command without expecting any response (fire-and-forget).
     *
     * <p>
     * The default timeout value is globally configured via the property
     * {@code splitr.bus.default-timeout} (Defaulting to 10ms if not specified).
     * </p>
     *
     * @param command The command payload object to be dispatched.
     * @param timeoutMs The maximum time to wait for the response in milliseconds.
     */
    void publish(Object command, long timeoutMs);
}
