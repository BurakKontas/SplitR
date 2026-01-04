package tr.kontas.splitr.saga.exceptions;

public class SagaNotFoundException extends SagaException {
    public SagaNotFoundException(String correlationId) {
        super("Saga not found with correlationId: " + correlationId);
    }
}
