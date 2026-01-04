package tr.kontas.splitr.saga.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a Saga, enabling distributed transaction management
 * with automatic compensation, retry, and completion handling.
 *
 * <p>A saga represents a long-running business transaction that can span multiple services.
 * This annotation allows configuring persistence, compensation behavior, retries, timeouts,
 * concurrency, and logging.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Saga(
 *     name = "orderSaga",
 *     autoCompensate = true,
 *     retryOnFailure = true,
 *     retryAttempts = 5
 * )
 * public class OrderSagaHandler { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Saga {

    /**
     * Optional name for the saga. Useful for logging, monitoring, or identifying
     * saga instances.
     *
     * <p>
     * If not provided, a default name based on the class or method name will be used.
     * </p>
     */
    String name() default "";

    /**
     * Optional tags for categorizing the saga.
     */
    String[] tags() default {};

    /**
     * Indicates whether the saga state should be persisted.
     */
    boolean isPersistent() default true;

    /**
     * Persistence strategy for the saga state.
     * <p>Supported values include:</p>
     * <ul>
     *     <li><b>DB</b> - relational database using JPA/Hibernate (Default)</li>
     *     <li><b>MEMORY</b> - in-memory storage (for testing or ephemeral use cases)</li>
     *     <li><b>CUSTOM</b> - user-defined storage</li>
     * </ul>
     * <p>
     * If set to "CUSTOM", the {@link #customPersistenceBean()} must be provided.
     * </p>
     */
    String persistenceStrategy() default "DB";

    /**
     * Bean name for custom persistence implementation if
     * {@link #persistenceStrategy()} is set to "CUSTOM".
     */
    String customPersistenceBean() default "";

    /**
     * Enables automatic compensation if a saga step fails.
     */
    boolean autoCompensate() default true;

    /**
     * Enables automatic completion of the saga when all steps succeed.
     */
    boolean autoComplete() default true;

    /**
     * Enables automatic retry of failed steps.
     * <p>
     * When enabled, the following properties are relevant:
     * <ul>
     *     <li>{@link #retryAttempts()}</li>
     *     <li>{@link #retryDelayMs()}</li>
     *     <li>{@link #retryDelayMultiplier()}</li>
     *     <li>{@link #retryWithJitter()}</li>
     * </ul>
     * </p>
     */
    boolean retryOnFailure() default false;

    /**
     * Maximum number of retry attempts for failed steps.
     * <p>Relevant only if {@link #retryOnFailure()} is true.</p>
     */
    int retryAttempts() default 5;

    /**
     * Base delay in milliseconds before retrying a failed step.
     * <p>Relevant only if {@link #retryOnFailure()} is true.</p>
     */
    long retryDelayMs() default 1000;

    /**
     * Multiplier for exponential backoff in retries.
     * <p>Relevant only if {@link #retryOnFailure()} is true.</p>
     */
    long retryDelayMultiplier() default 2;

    /**
     * Enables random jitter for retry delay to avoid thundering herd problems.
     * <p>Relevant only if {@link #retryOnFailure()} is true.</p>
     */
    boolean retryWithJitter() default false;

    /**
     * Enables compensation if the saga exceeds a timeout.
     * <p>Related to {@link #timeoutMs()}.</p>
     */
    boolean timeoutCompensate() default false;

    /**
     * Timeout in milliseconds for the saga. A value of -1 indicates no timeout.
     * <p>Relevant for {@link #timeoutCompensate()} behavior.</p>
     */
    long timeoutMs() default -1;

    /**
     * Allows multiple instances of the same saga to run concurrently.
     */
    boolean allowConcurrent() default false;

    /**
     * Exceptions that should be ignored when triggering compensation.
     */
    Class<? extends Throwable>[] ignoreForCompensation() default {};

    /**
     * Enables logging of saga progress for monitoring purposes.
     */
    boolean logProgress() default true;

    /// Callback URL to send saga state updates (will send all SagaState if callbackOnStatuses is set)
    Callback callback() default @Callback;

    /// Callback only on specified statuses
    String[] callbackOnStatuses() default {};

    /**
     * Indicates if the callback should be sent as a Server-Sent Event (SSE).
     *
     * @return true if the callback is an SSE, false otherwise
     */
    boolean isSSE() default false;
}
