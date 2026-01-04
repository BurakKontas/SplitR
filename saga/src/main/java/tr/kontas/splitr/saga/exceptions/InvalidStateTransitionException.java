package tr.kontas.splitr.saga.exceptions;

public class InvalidStateTransitionException extends SagaException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
