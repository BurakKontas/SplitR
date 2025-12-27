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
public interface QueryHandler<T> extends BusHandler<T> { }
